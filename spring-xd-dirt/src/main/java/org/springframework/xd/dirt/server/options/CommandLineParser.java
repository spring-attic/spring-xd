/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.server.options;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import org.springframework.xd.dirt.server.options.ParserEvent.Event;


/**
 * 
 * @author David Turanski
 */
public class CommandLineParser extends CmdLineParser {


	private ParserEventListener parserEventListener;

	/**
	 * @param options
	 */
	public CommandLineParser(AbstractOptions options) {
		super(options);
		this.parserEventListener = options.getParserEventListener();
	}

	@Override
	/**
	 * returns true if this is an option rather than a command line argument
	 */
	protected boolean isOption(String arg) {
		boolean isOption = super.isOption(arg);
		if (isOption) {
			parserEventListener.handleEvent(new ParserArgEvent(arg.replaceAll("-", ""), Event.option_explicit));
		}
		return isOption;

	}

	@Override
	public void parseArgument(String... args) {
		try {
			parserEventListener.handleEvent(new ParserLifeCycleEvent(Event.parser_started));
			super.parseArgument(args);
			parserEventListener.handleEvent(new ParserLifeCycleEvent(Event.parser_complete));
		}
		catch (CmdLineException e) {
			throw new InvalidCommandLineArgumentException(e.getMessage(), e);
		}
	}
}
