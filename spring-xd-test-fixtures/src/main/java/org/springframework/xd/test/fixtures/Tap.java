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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * Test fixture that creates a tap.
 * @author Glenn Renfro
 */
public class Tap extends AbstractModuleFixture<Tap> {

	private String streamName;

	/**
	 * Create a tap stream with the provided name.
	 *
	 * @param streamName name of the tap stream.
	 */
	public Tap(String streamName) {
		Assert.hasText(streamName, "StreamName must not be empty nor null");
		this.streamName = streamName;
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		StringBuilder dsl = new StringBuilder("tap:stream:");
		dsl.append(streamName);
		if (label != null) {
			dsl.append(".");
			dsl.append(label);
		}
		return dsl.toString();
	}

}
