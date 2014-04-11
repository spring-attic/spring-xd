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

package org.springframework.xd.shell.command;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.FileSink;


/**
 * Runs tests for the router sink module.
 * 
 * @author Eric Bottard
 */
public class RouterSinkTests extends AbstractStreamIntegrationTest {

	@Test
	public void testUsingExpression() {
		FileSink fileSink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();

		stream().create("f", "queue:foo > transform --expression=payload+'-foo' | %s", fileSink);
		stream().create("b", "queue:bar > transform --expression=payload+'-bar' | %s", fileSink);
		stream().create("r", "%s | router --expression=payload.contains('a')?'queue:foo':'queue:bar'", httpSource);

		httpSource.ensureReady().postData("a").postData("b");

		assertThat(fileSink, eventually(hasContentsThat(equalTo("a-foob-bar"))));

	}

}
