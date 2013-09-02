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

package org.springframework.xd.shell.command;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.HttpSource;


/**
 * Shell integration tests for various simple processor modules.
 * 
 * @author Eric Bottard
 */
public class ProcessorsTests extends AbstractStreamIntegrationTest {

	@Test
	public void splitterDoesNotSplitByDefault() throws Exception {
		HttpSource httpSource = newHttpSource();

		stream().create("splitter-test", "%s | splitter | counter --name=%s", httpSource, DEFAULT_METRIC_NAME);

		httpSource.ensureReady().postData("Hello World !");
		counter().verifyCounter("1");

	}

	@Test
	public void splitterDoesSplit() {
		HttpSource httpSource = newHttpSource();

		stream().create("splitter-test", "%s | splitter --expression=payload.split(' ') | counter --name=%s",
				httpSource, DEFAULT_METRIC_NAME);

		httpSource.ensureReady().postData("Hello World !");
		counter().verifyCounter("3");

	}

}
