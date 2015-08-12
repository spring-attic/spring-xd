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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.IOException;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.EventuallyMatcher;
import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.fixtures.TcpSink;
import org.springframework.xd.test.fixtures.TcpSource;


/**
 * Tests for tcp source and sink.
 *
 * @author Eric Bottard
 * @author Patrick Peralta
 */
public class AbstractTcpModulesTests extends AbstractStreamIntegrationTest {

	@Test
	public void testTcpSource() throws Exception {
		TcpSource tcpSource = newTcpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(), "%s | %s", tcpSource, fileSink);

		// Following \r\n is because of CRLF deserializer
		tcpSource.ensureReady().sendBytes("Hello\r\n".getBytes("UTF-8"));
		assertThat(fileSink, eventually(hasContentsThat(equalTo("Hello"))));
	}

	@Test
	public void testTcpSink() throws Exception {
		TcpSink tcpSink = newTcpSink().start();
		HttpSource httpSource = newHttpSource();

		stream().create(generateStreamName(), "%s | %s", httpSource, tcpSink);
		httpSource.ensureReady().postData("Hi there!");

		// The following CRLF is b/c of the default tcp serializer
		// NOT because of FileSink
		assertThat(tcpSink, eventually(tcpSinkReceived("Hi there!\r\n")));
	}

	private EventuallyMatcher<TcpSink> tcpSinkReceived(String value) {
		return new EventuallyMatcher<TcpSink>(new TcpSinkContentsMatcher(value));
	}


	/**
	 * Matcher for {@link TcpSink} content.
	 */
	private class TcpSinkContentsMatcher extends BaseMatcher<TcpSink> {
		private final String expected;

		private TcpSinkContentsMatcher(String expected) {
			this.expected = expected;
		}

		private String getSinkContents(Object sink) {
			TcpSink tcpSink = (TcpSink) sink;
			try {
				return new String(tcpSink.getReceivedBytes(), "ISO-8859-1");
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean matches(Object sink) {
			return getSinkContents(sink).equals(this.expected);
		}

		@Override
		public void describeMismatch(Object sink, Description description) {
			description.appendText("was ").appendValue(getSinkContents(sink));
		}

		@Override
		public void describeTo(Description description) {
			description.appendText(this.expected);
		}
	}

}
