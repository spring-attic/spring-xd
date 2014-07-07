/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.util;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.xd.shell.command.ConfigCommands;
import org.springframework.xd.shell.command.HttpCommands;
import org.springframework.xd.shell.command.JobCommands;
import org.springframework.xd.shell.hadoop.ConfigurationCommands;
import org.springframework.xd.shell.hadoop.FsShellCommands;


/**
 * Will check that our command classes properly implement {@code CliAvailabilityIndicator}
 * (assumes all commands require XD "connectivity") unless explicitly excluded.
 */
public class CliAvailabilityIndicatorChecker {

	private static final List<Class<? extends CommandMarker>> EXCLUDES = Arrays.asList(
			HttpCommands.class, FsShellCommands.class, ConfigurationCommands.class, ConfigCommands.class,
			JobCommands.class);

	/**
	 * A filter that matches the method(s) that bear some annotation.
	 */
	private final class AnnotationBearingMethodFilter implements MethodFilter {

		private Class<? extends Annotation> clazz;

		public AnnotationBearingMethodFilter(Class<? extends Annotation> clazz) {
			this.clazz = clazz;
		}

		@Override
		public boolean matches(Method method) {
			return method.getAnnotation(clazz) != null;
		}
	}

	/**
	 * A method collector that will retrieve the (String[]) value of the {@code value()} attribute of some annotation
	 * that methods are known to bear.
	 */
	private final class AnnotationValueMethodCollector<A extends Annotation> implements MethodCallback {

		private Set<String> commandNames;

		private Class<A> clazz;

		public AnnotationValueMethodCollector(Class<A> clazz, Set<String> commandNames) {
			this.clazz = clazz;
			this.commandNames = commandNames;
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
			A annotation = method.getAnnotation(clazz);
			commandNames.addAll(Arrays.asList((String[]) AnnotationUtils.getValue(annotation)));
		}

	}

	@Test
	public void checkCliAvailabilityIndicator() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getBeanFactory().registerSingleton("commandLine", new CommandLine(null, 100, null, false));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions("classpath*:META-INF/spring/spring-shell-plugin.xml");
		ctx.refresh();

		Collection<CommandMarker> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, CommandMarker.class).values();
		for (Iterator<CommandMarker> it = beans.iterator(); it.hasNext();) {
			if (EXCLUDES.contains(it.next().getClass())) {
				it.remove();
			}
		}

		final MethodFilter commandMethodfilter = new AnnotationBearingMethodFilter(CliCommand.class);

		final MethodFilter availabilityMethodfilter = new AnnotationBearingMethodFilter(CliAvailabilityIndicator.class);

		for (CommandMarker plugin : beans) {
			Set<String> commandNames = new TreeSet<String>();
			Set<String> namesUsedInAvailabilityIndicator = new TreeSet<String>();
			ReflectionUtils.doWithMethods(plugin.getClass(),
					new AnnotationValueMethodCollector<CliCommand>(CliCommand.class, commandNames),
					commandMethodfilter);
			ReflectionUtils.doWithMethods(plugin.getClass(),
					new AnnotationValueMethodCollector<CliAvailabilityIndicator>(CliAvailabilityIndicator.class,
							namesUsedInAvailabilityIndicator),
							availabilityMethodfilter);

			TreeSet<String> copy = new TreeSet<String>(commandNames);
			copy.removeAll(namesUsedInAvailabilityIndicator);
			String explanation = String.format(
					"The method bearing @%s in %s is missing the following command names: %s",
					CliAvailabilityIndicator.class.getSimpleName(), plugin.getClass(), copy);
			assertTrue(explanation, namesUsedInAvailabilityIndicator.equals(commandNames));


			if (!commandNames.equals(namesUsedInAvailabilityIndicator)) {
				System.out.println(plugin);
				System.out.println(namesUsedInAvailabilityIndicator);
				System.out.println(commandNames);
				System.out.println();
			}
		}

	}
}
