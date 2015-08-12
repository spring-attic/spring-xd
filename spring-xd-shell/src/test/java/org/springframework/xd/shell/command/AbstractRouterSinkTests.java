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
 * @author Ilayaperumal Gopinathan
 */
public class AbstractRouterSinkTests extends AbstractStreamIntegrationTest {

	@Test
	public void testUsingExpression() {
		FileSink fileSink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		String queue1 = generateQueueName();
		String queue2 = generateQueueName();

		stream().create(generateStreamName(), "%s > transform --expression=payload+'-foo' | %s", queue1, fileSink);
		stream().create(generateStreamName(), "%s > transform --expression=payload+'-bar' | %s", queue2, fileSink);
		stream().create(generateStreamName(),
				"%s | router --expression=payload.contains('a')?'" + queue1 + "':'" + queue2 + "'",
				httpSource);

		httpSource.ensureReady();
		httpSource.postData("a");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a-foo"))));
		httpSource.postData("b");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a-foob-bar"))));
	}

	@Test
	public void testUsingScript() {
		FileSink fileSink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();

		stream().create(generateStreamName(), "queue:testUsingScript1 > transform --expression=payload+'-foo' | %s", fileSink);
		stream().create(generateStreamName(), "queue:testUsingScript2 > transform --expression=payload+'-bar' | %s", fileSink);
		stream().create(generateStreamName(),
				"%s | router --script='org/springframework/xd/shell/command/router.groovy'", httpSource);

		httpSource.ensureReady();
		httpSource.postData("a");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a-foo"))));
		httpSource.postData("b");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a-foob-bar"))));
	}

	@Test
	public void testDiscardsUsingExpression() {
		FileSink fileSink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		String queue = generateQueueName();

		stream().create(generateStreamName(),
				"%s | router --expression='payload.contains(\"a\") ? \"%s\" : null'", httpSource, queue);
		stream().create(generateStreamName(), "%s > %s", queue, fileSink);

		httpSource.ensureReady().postData("a1");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a1"))));
		httpSource.postData("b1").postData("b2").postData("a2");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a1a2"))));
	}

	@Test
	public void testDiscardsUsingScript() {
		FileSink fileSink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		String queue = "queue:my-queue";

		stream().create(generateStreamName(),
				"%s | router --script='org/springframework/xd/shell/command/router-discard.groovy'", httpSource, queue);
		stream().create(generateStreamName(), "%s > %s", queue, fileSink);

		httpSource.ensureReady().postData("a1");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a1"))));
		httpSource.postData("b1").postData("b2").postData("a2");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("a1a2"))));
	}

}
