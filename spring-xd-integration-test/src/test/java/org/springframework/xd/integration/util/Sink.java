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

package org.springframework.xd.integration.util;


/**
 * Utility class for setting up the sinks required for testing.
 * 
 * @author renfrg
 */
public class Sink {

	public static final Sink FILE = new Sink("file --mode=REPLACE", SinkType.file);

	public static final Sink LOG = new Sink("log", SinkType.log);

	private String sinkText;

	private SinkType sinkType;

	public Sink(String sinkText, SinkType sinkType) {
		this.sinkText = sinkText;
		this.sinkType = sinkType;
	}

	@Override
	public String toString() {
		return sinkText;
	}

	public String getSinkText() {
		return sinkText;
	}

	public SinkType getSinkType() {
		return sinkType;
	}
}
