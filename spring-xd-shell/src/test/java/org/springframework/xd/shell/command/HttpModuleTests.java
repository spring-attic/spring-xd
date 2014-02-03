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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import javax.xml.transform.dom.DOMSource;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;


/**
 * Tests the http source module.
 * 
 * @author Eric Bottard
 */
public class HttpModuleTests extends AbstractStreamIntegrationTest {

	@Test
	public void testImplicitContentConversionStringy() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(), "%s | %s", httpSource, fileSink);

		// Those japanese chars should render OK
		httpSource.ensureReady().useContentType("text/plain;Charset=UTF-8").postData("Hello \u3044\u3002");

		// Those japanese chars should not
		httpSource.ensureReady().useContentType("text/plain;Charset=ISO-8859-1").postData("Hello \u3044\u3002");
		assertThat(fileSink, eventually(hasContentsThat(is(equalTo("Hello \u3044\u3002Hello ??")))));
	}

	@Test
	public void testImplicitContentConversionBinary() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(), "%s | transform --expression=payload.getClass().toString() | %s",
				httpSource,
				fileSink);

		httpSource.ensureReady().useContentType("image/jpeg").postData("Hello");

		assertThat(fileSink, eventually(hasContentsThat(is(equalTo(byte[].class.toString())))));
	}

	@Test
	public void testExplicitContentConversion() {
		HttpSource httpSource = newHttpSource().unmarshallTo(DOMSource.class);
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --expression=payload.getNode().getFirstChild().getTextContent() | %s",
				httpSource,
				fileSink);

		httpSource.ensureReady().useContentType("text/xml").postData("<tag>Hello</tag>");

		assertThat(fileSink, eventually(hasContentsThat(is(equalTo("Hello")))));

	}
}
