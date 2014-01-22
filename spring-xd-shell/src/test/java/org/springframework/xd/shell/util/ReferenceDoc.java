/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.command.AggregateCounterCommands;
import org.springframework.xd.shell.command.CounterCommands;
import org.springframework.xd.shell.command.FieldValueCounterCommands;
import org.springframework.xd.shell.command.GaugeCommands;
import org.springframework.xd.shell.command.HttpCommands;
import org.springframework.xd.shell.command.JobCommands;
import org.springframework.xd.shell.command.ModuleCommands;
import org.springframework.xd.shell.command.RichGaugeCommands;
import org.springframework.xd.shell.command.RuntimeCommands;
import org.springframework.xd.shell.command.StreamCommands;
import org.springframework.xd.shell.hadoop.ConfigurationCommands;
import org.springframework.xd.shell.hadoop.FsShellCommands;
import org.springframework.xd.shell.properties.SpringConfigurationPropertiesCommands;

/**
 * A quick and dirty tool to collect command help() text and generate an asciidoc page. Also enforces some constraints
 * on commands. Can be run as a unit test to enforce constraints only.
 * 
 * @author Eric Bottard
 * 
 */
public class ReferenceDoc {

	private final static Pattern COMMAND_FORMAT = Pattern.compile("[a-z][a-zA-Z ]+");

	private final static Pattern OPTION_FORMAT = Pattern.compile("[a-z][a-zA-Z ]+");

	/* Enforce uppercase first, prevent final dot. */
	private final static Pattern COMMAND_HELP = Pattern.compile("[A-Z].+[^\\.]$");

	/* Enforce lowercase first, prevent final dot. */
	private final static Pattern OPTION_HELP = Pattern.compile("[a-z].+[^\\.]$");

	/**
	 * A mapping from class to Title in the doc. Insertion order will become rendering order.
	 */
	private Map<Class<? extends CommandMarker>, String> titles = new LinkedHashMap<Class<? extends CommandMarker>, String>();

	private PrintStream out = new PrintStream(new NullOutputStream());

	private static class NullOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
		}

	}

	public ReferenceDoc() {
		/*
		 * Set titles for commands. Please note that insertion order matters!
		 */
		titles.put(XDShell.class, "Base Commands");
		// ===== Runtime Containers/Modules ======
		titles.put(RuntimeCommands.class, "Runtime Commands");

		// ===== Streams etc. ======
		titles.put(StreamCommands.class, "Stream Commands");
		titles.put(JobCommands.class, "Job Commands");
		titles.put(ModuleCommands.class, "Module Commands");

		// ======= Analytics =======
		// Use of repeated title here on purpose
		titles.put(CounterCommands.class, "Metrics Commands");
		titles.put(FieldValueCounterCommands.class, "Metrics Commands");
		titles.put(AggregateCounterCommands.class, "Metrics Commands");
		titles.put(GaugeCommands.class, "Metrics Commands");
		titles.put(RichGaugeCommands.class, "Metrics Commands");

		// ======= Http Post =======
		titles.put(HttpCommands.class, "Http Commands");

		// ======== Hadoop =========
		titles.put(ConfigurationCommands.class, "Hadoop Configuration Commands");
		titles.put(FsShellCommands.class, "Hadoop FileSystem Commands");

		titles.put(SpringConfigurationPropertiesCommands.class, "Spring Boot Configuration Commands");
	}

	private static final class CommandsCollector implements MethodCallback {

		private final Map<CliCommand, List<CliOption>> commands;

		private CommandsCollector(Map<CliCommand, List<CliOption>> commands) {
			this.commands = commands;
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
			List<CliOption> list = new ArrayList<CliOption>();
			commands.put(method.getAnnotation(CliCommand.class), list);
			for (Annotation[] anns : method.getParameterAnnotations()) {
				for (Annotation ann : anns) {
					if (ann instanceof CliOption) {
						list.add((CliOption) ann);
						break;
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		ReferenceDoc doc = new ReferenceDoc();
		doc.out = args.length == 1 ? new PrintStream(args[0]) : System.out;

		doc.doIt();

	}

	@Test
	public void doIt() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getBeanFactory().registerSingleton("commandLine", new CommandLine(null, 100, null));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions("classpath*:META-INF/spring/spring-shell-plugin.xml");
		ctx.refresh();

		Comparator<Class<? extends CommandMarker>> comparator = new Comparator<Class<? extends CommandMarker>>() {

			@Override
			public int compare(Class<? extends CommandMarker> arg0, Class<? extends CommandMarker> arg1) {
				List<Class<? extends CommandMarker>> sorted = new ArrayList<Class<? extends CommandMarker>>(
						titles.keySet());
				int diff = sorted.indexOf(arg0) - sorted.indexOf(arg1);
				return diff != 0 ? diff : arg0.getSimpleName().compareTo(arg1.getSimpleName());
			}
		};

		final Map<Class<? extends CommandMarker>, Map<CliCommand, List<CliOption>>> plugins = new TreeMap<Class<? extends CommandMarker>, Map<CliCommand, List<CliOption>>>(
				comparator);

		Map<String, CommandMarker> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, CommandMarker.class);
		final MethodFilter filter = new MethodFilter() {

			@Override
			public boolean matches(Method method) {
				return method.getAnnotation(CliCommand.class) != null;
			}
		};

		for (CommandMarker plugin : beans.values()) {
			LinkedHashMap<CliCommand, List<CliOption>> commands = new LinkedHashMap<CliCommand, List<CliOption>>();
			plugins.put(plugin.getClass(), commands);
			ReflectionUtils.doWithMethods(plugin.getClass(), new CommandsCollector(commands), filter);
		}

		String lastTitleUsed = null;
		for (Class<? extends CommandMarker> plugin : plugins.keySet()) {
			// == Stream Commands
			String title = titleFor(plugin);
			if (lastTitleUsed == null || !lastTitleUsed.equals(title)) {
				out.printf("=== %s%n", title);
				lastTitleUsed = title;
			}
			Map<CliCommand, List<CliOption>> commands = plugins.get(plugin);

			for (CliCommand command : commands.keySet()) {
				// === stream create
				out.printf("==== %s%n", check(command.value()[0], COMMAND_FORMAT));
				// Create a new stream definition.
				out.printf("%s.%n%n", check(command.help(), COMMAND_HELP));
				// stream create [--name]=<name> [--definition=<definition>]
				out.printf("    %s", command.value()[0]);
				for (CliOption option : commands.get(command)) {
					String paramName = check(paramName(option), OPTION_FORMAT);
					String optionText = String.format("<%s>", paramName);
					if (valueOptional(option)) {
						optionText = String.format("--%s [%s]", paramName, optionText);
					}
					else if (keyOptional(option)) {
						optionText = String.format("[--%s] %s", paramName, optionText);
					}
					else {
						optionText = String.format("--%s %s", paramName, optionText);
					}
					if (!option.mandatory()) {
						optionText = String.format("[%s]", optionText);
					}
					if (option.mandatory() && valueOptional(option)) {
						// This combination does not make sense. Or does it?
						throw new IllegalStateException("" + command + " " + option);
					}
					out.printf(" %s", optionText);
				}
				out.println("");
				out.println();

				// *definition*:: the stream definition
				for (CliOption option : commands.get(command)) {
					out.printf("*%s*:: %s.", paramName(option), check(option.help(), OPTION_HELP));
					if (!option.mandatory()) {
						// There can be non-mandatory, w/o default options (e.g. mutually exclusive, with 1 required,
						// options)
						if (!"__NULL__".equals(option.unspecifiedDefaultValue())) {
							out.printf(" *(default: `%s`", option.unspecifiedDefaultValue());
							if (valueOptional(option)) {
								if (option.specifiedDefaultValue().equals(option.unspecifiedDefaultValue())) {
									throw new IllegalStateException("" + option);
								}

								out.printf(", or `%s` if +--%s+ is specified without a value",
										option.specifiedDefaultValue(), paramName(option));
							}
							out.printf(")*");
						}
					}
					else {
						out.printf(" *(required)*");
					}
					out.printf("%n");
				}

				out.println();
			}
			out.println();
		}
		ctx.close();
	}

	private boolean valueOptional(CliOption option) {
		return !"__NULL__".equals(option.specifiedDefaultValue());
	}

	private String titleFor(Class<? extends CommandMarker> plugin) {
		if (!titles.containsKey(plugin)) {
			throw new IllegalArgumentException("Missing title for " + plugin);
		}
		return titles.get(plugin);
	}

	private boolean keyOptional(CliOption option) {
		return Arrays.asList(option.key()).contains("");
	}

	private String check(String candidate, Pattern regex) {
		if (!regex.matcher(candidate).matches()) {
			throw new IllegalArgumentException(candidate + " should match " + regex);
		}
		return candidate;
	}

	private String paramName(CliOption option) {
		String[] possibleValues = option.key();
		for (String s : possibleValues) {
			if (!"".equals(s)) {
				return s;
			}
		}
		throw new IllegalStateException("Option should have a non empty key: " + option.help());
	}
}
