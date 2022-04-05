/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server.options;

import static java.lang.Boolean.TRUE;
import static org.springframework.core.env.CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

/**
 * An {@code ApplicationListener} that will parse command line options and also replace the default boot commandline
 * {@link PropertySource} with those values. This turns out to be the most elegant solution if we want to keep the
 * {@link SpringApplicationBuilder} code clean.
 * 
 * @author Eric Bottard
 * @author Luke Taylor
 */
public class CommandLinePropertySourceOverridingListener<T extends CommonOptions> implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private T options;

	private static ThreadLocal<Environment> environmentHolder = new ThreadLocal<Environment>();

	public CommandLinePropertySourceOverridingListener(T options) {
		super();
		this.options = options;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		if (event.getArgs().length == 0) {
			// If there are no arguments, create cmdLineArgs property source here. This would at least hold the default
			// command line options.
			if (event.getEnvironment().getPropertySources().get(COMMAND_LINE_PROPERTY_SOURCE_NAME) == null) {
				event.getEnvironment().getPropertySources().addFirst(new SimpleCommandLinePropertySource());
			}
		}
		CmdLineParser parser = null;
		try {
			environmentHolder.set(event.getEnvironment());
			parser = new CmdLineParser(options);
			parser.parseArgument(event.getArgs());
			if (TRUE.equals(options.isShowHelp())) {
				System.err.println("Usage:");
				parser.printUsage(System.err);
				System.exit(0);
			}
		}
		catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println("Usage:");
			parser.printUsage(System.err);
			System.exit(1);
		}
		finally {
			environmentHolder.set(null);
		}

		final EnumerablePropertySource<T> ps = new BeanPropertiesPropertySource<T>(COMMAND_LINE_PROPERTY_SOURCE_NAME,
				options);

		// Convert all properties to their String representation
		// Also, don't advertise property names for which value is null
		Map<String, Object> map = new HashMap<String, Object>();
		for (String key : ps.getPropertyNames()) {
			Object raw = ps.getProperty(key);
			if (raw != null) {
				map.put(key, raw.toString());
			}
		}

		event.getEnvironment().getPropertySources().replace(COMMAND_LINE_PROPERTY_SOURCE_NAME,
				new MapPropertySource(COMMAND_LINE_PROPERTY_SOURCE_NAME, map));

	}

	/*default*/static Environment getCurrentEnvironment() {
		return environmentHolder.get();
	}

}
