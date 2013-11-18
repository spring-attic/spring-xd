/*
 * Copyright 2011-2012 the original author or authors.
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

package org.springframework.xd.shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.shell.CommandLine;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.ExitShellRequest;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.core.Shell;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.util.StopWatch;

/**
 * Loads a {@link Shell} using Spring IoC container for the XD Shell.
 * 
 * @author Glenn Renfro
 * 
 */
public class XDShellBootStrap {

	private final static String[] CONTEXT_PATH = { "classpath*:/META-INF/spring/spring-shell-plugin.xml" };

	private static XDShellBootStrap bootstrap;

	private static StopWatch sw = new StopWatch("Spring XD Shell");

	private static CommandLine commandLine;

	private GenericApplicationContext ctx;


	private static final Logger LOGGER = HandlerUtils.getLogger(XDShellBootStrap.class);

	public static final int DEFAULT_HISTORY_SIZE = 3000;


	public static void main(String[] args) throws IOException {
		sw.start();
		ExitShellRequest exitShellRequest;
		try {
			bootstrap = new XDShellBootStrap(args);
			exitShellRequest = bootstrap.run();
		}
		catch (IllegalArgumentException iae) {
			LOGGER.info(iae.getMessage());
			exitShellRequest = ExitShellRequest.FATAL_EXIT;
		}
		catch (RuntimeException t) {
			throw t;
		}
		finally {
			HandlerUtils.flushAllHandlers(Logger.getLogger(""));
		}

		System.exit(exitShellRequest.getExitCode());
	}

	public XDShellBootStrap(String[] args) throws IOException {
		this(args, CONTEXT_PATH);
	}

	public XDShellBootStrap(String[] args, String[] contextPath) {
		try {
			commandLine = parseCommandLine(args);
		}
		catch (IOException e) {
			throw new ShellException(e.getMessage(), e);
		}

		ctx = new GenericApplicationContext();
		ctx.registerShutdownHook();
		configureApplicationContext(ctx);
		// built-in commands and converters
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("org.springframework.shell.commands", "org.springframework.shell.converters",
				"org.springframework.shell.plugin.support");
		// user contributed commands
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(contextPath);
		ctx.refresh();
	}


	public ApplicationContext getApplicationContext() {
		return ctx;
	}

	/**
	 * Creates and registers all converter beans necessary for XDShell. It also sets up the commandline bean.
	 * 
	 * @param annctx
	 */
	private void configureApplicationContext(GenericApplicationContext annctx) {
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.StringConverter.class);
		createAndRegisterBeanDefinition(annctx,
				org.springframework.shell.converters.AvailableCommandsConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.BigDecimalConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.BigIntegerConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.BooleanConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.CharacterConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.DateConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.DoubleConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.EnumConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.FloatConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.IntegerConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.LocaleConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.LongConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.ShortConverter.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.StaticFieldConverterImpl.class);
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.core.JLineShellComponent.class, "shell");
		createAndRegisterBeanDefinition(annctx, org.springframework.shell.converters.SimpleFileConverter.class);

		annctx.getBeanFactory().registerSingleton("commandLine", commandLine);
	}

	protected void createAndRegisterBeanDefinition(GenericApplicationContext annctx, Class<?> clazz) {
		createAndRegisterBeanDefinition(annctx, clazz, null);
	}

	protected void createAndRegisterBeanDefinition(GenericApplicationContext annctx, Class<?> clazz, String name) {
		RootBeanDefinition rbd = new RootBeanDefinition();
		rbd.setBeanClass(clazz);
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) annctx.getBeanFactory();
		if (name != null) {
			bf.registerBeanDefinition(name, rbd);
		}
		else {
			bf.registerBeanDefinition(clazz.getSimpleName(), rbd);
		}
	}

	/**
	 * Determines if the shell should run a command(s) and then terminate or bring up the prompt.
	 * 
	 * @return The status of the run.
	 */
	protected ExitShellRequest run() {

		String[] commandsToExecuteAndThenQuit = commandLine.getShellCommandsToExecute();
		// The shell is used
		JLineShellComponent shell = ctx.getBean("shell", JLineShellComponent.class);
		ExitShellRequest exitShellRequest;

		if (null != commandsToExecuteAndThenQuit) {
			boolean successful = false;
			exitShellRequest = ExitShellRequest.FATAL_EXIT;

			for (String cmd : commandsToExecuteAndThenQuit) {
				successful = shell.executeCommand(cmd).isSuccess();
				if (!successful)
					break;
			}

			// if all commands were successful, set the normal exit status
			if (successful) {
				exitShellRequest = ExitShellRequest.NORMAL_EXIT;
			}
		}
		else {
			shell.start();
			shell.promptLoop();
			exitShellRequest = shell.getExitShellRequest();
			if (exitShellRequest == null) {
				// shouldn't really happen, but we'll fallback to this anyway
				exitShellRequest = ExitShellRequest.NORMAL_EXIT;
			}
			shell.waitForComplete();
		}

		ctx.close();
		sw.stop();
		if (shell.isDevelopmentMode()) {
			System.out.println("Total execution time: " + sw.getLastTaskTimeMillis() + " ms");
		}
		return exitShellRequest;
	}

	public JLineShellComponent getJLineShellComponent() {
		return ctx.getBean("shell", JLineShellComponent.class);
	}

	/**
	 * Reviews the command line and does basic validation on the elements. Also displays help with the --help command.
	 * 
	 * @param args The command line to be parsed.
	 * @return Command Line object containing the shell params as well as XD specific settings.
	 * @throws IOException
	 */
	protected CommandLine parseCommandLine(String[] args)
			throws IOException {
		if (args == null) {
			args = new String[] {};
		}
		List<String> commands = new ArrayList<String>();
		Map<String, String> options = new HashMap<String, String>();
		int historySize = DEFAULT_HISTORY_SIZE;
		String[] executeThenQuit = null;

		int i = 0;
		while (i < args.length) {
			String arg = args[i++];
			// Extract Spring Shell command Line Parameters
			if (arg.equals("--profiles")) {
				try {
					String profiles = args[i++];
					options.put("spring.profiles.active", profiles);
				}
				catch (ArrayIndexOutOfBoundsException e) {
					LOGGER.warning("No value specified for --profiles option");
				}
			}
			else if (arg.equals("--cmdfile")) {
				try {
					File f = new File(args[i++]);
					commands.addAll(FileUtils.readLines(f));
				}
				catch (IOException e) {
					LOGGER.warning("Could not read lines from command file: " + e.getMessage());
				}
				catch (ArrayIndexOutOfBoundsException e) {
					LOGGER.warning("No value specified for --cmdfile option");
				}
			}
			else if (arg.equals("--histsize")) {
				try {
					String histSizeArg = args[i++];
					int histSize = Integer.parseInt(histSizeArg);
					if (histSize <= 0) {
						LOGGER.warning("histsize option must be > 0, using default value of " + DEFAULT_HISTORY_SIZE);
					}
					else {
						historySize = histSize;
					}
				}
				catch (NumberFormatException e) {
					LOGGER.warning("Unable to parse histsize value to an integer ");
				}
				catch (ArrayIndexOutOfBoundsException ae) {
					LOGGER.warning("No value specified for --histsize option");
				}
			}
			// Skip XD Specific parameters so that they will not be included in the executeThenQuit commands
			// but go ahead and validate the entries
			else if (arg.equals("--host")) {
				if (i == args.length) {
					throw new IllegalArgumentException("No Host was specified for the --host param.");
				}
				i++;
			}
			else if (arg.equals("--port")) {
				try {
					int portVal = Integer.parseInt(args[i++]);
					if (portVal <= 0) {
						LOGGER.info("port option must be > 0, using default value");
					}
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException("Unable to parse port value to an integer.");
				}
				catch (ArrayIndexOutOfBoundsException aio) {
					throw new IllegalArgumentException("Port option must be > 0.");
				}

			}
			else if (arg.equals("--help")) {
				printUsage();
				System.exit(0);
			}
			else {
				i--;
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		for (; i < args.length; i++) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(args[i]);
		}

		if (sb.length() > 0) {
			String[] cmdLineCommands = sb.toString().split(";");
			for (String s : cmdLineCommands) {
				// add any command line commands after the commands loaded from the file
				commands.add(s.trim());
			}
		}

		if (commands.size() > 0) {
			executeThenQuit = commands.toArray(new String[commands.size()]);
		}

		for (Map.Entry<String, String> entry : options.entrySet()) {
			System.setProperty(entry.getKey(), entry.getValue());
		}

		return new CommandLine(args, historySize, executeThenQuit);
	}

	private void printUsage() {
		LOGGER.info("Usage:  --help --histsize [size] --cmdfile [file name] --profiles [comma-separated list of profile names] --host [xd admin server host] --port [port admin server port]");
	}


}
