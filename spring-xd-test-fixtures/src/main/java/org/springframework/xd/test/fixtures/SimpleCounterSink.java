/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.test.fixtures;

/**
 * Used to generate the counter sink portion of a stream. Used by Acceptance testing
 * because it does not extend AbstractMetricSink.
 *
 * @author Glenn Renfro
 */
public class SimpleCounterSink extends AbstractModuleFixture<SimpleFileSink> {
	private String name;

	public SimpleCounterSink(String name) {
		this.name = name;
	}

	@Override
	protected String toDSL() {
		return String.format("counter --name=%s", name);
	}
}
