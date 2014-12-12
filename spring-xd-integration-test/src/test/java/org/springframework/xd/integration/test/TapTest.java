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

package org.springframework.xd.integration.test;

import java.util.UUID;

import org.junit.Test;


/**
 * Verifies that the Taps receive data from a stream and can process the accordingly.
 *
 * @author Glenn Renfro
 */
public class TapTest extends AbstractIntegrationTest {

	/**
	 * Evaluates whether the tap can retrieve data from a stream.
	 *
	 */
	@Test
	public void testStreamTap() {
		String data = UUID.randomUUID().toString();
		stream("dataSender",
				sources.http() + XD_DELIMITER
						+ sinks.log());
		stream(sources.tap("dataSender") + XD_TAP_DELIMITER
				+ sinks.file());

		sources.httpSource("dataSender").postData(data);
		assertValid(data, sinks.file());
	}


	/**
	 * Evaluates whether the tap can retrieve data from a stream with a 2 processor using labels.
	 */
	@Test
	public void testStreamTapTwoProcessors() {
		String data = UUID.randomUUID().toString();
		stream("dataSender",
				sources.http() + XD_DELIMITER
						+ processors.transform().expression("payload.toUpperCase()").label("label1")
						+ XD_DELIMITER + processors.transform().expression("payload.substring(3)").label("label2")
						+ XD_DELIMITER
						+ sinks.log());
		stream(sources.tap("dataSender").label("label2") + XD_TAP_DELIMITER
				+ sinks.file());

		sources.httpSource("dataSender").postData(data);
		assertValid(data.toUpperCase().substring(3), sinks.file());
	}

	/**
	 * Evaluates whether the tap can retrieve data from a stream with two processors and one is labeled.
	 */
	@Test
	public void testStreamLabel() {
		String data = UUID.randomUUID().toString();
		stream("dataSender",
				sources.http() + XD_DELIMITER
						+ processors.transform().expression("payload.toUpperCase()").label("ds1")
						+ XD_DELIMITER + processors.transform().expression("payload.substring(3)") + XD_DELIMITER
						+ sinks.log());
		stream(sources.tap("dataSender").label("ds1") + XD_TAP_DELIMITER
				+ sinks.file());

		sources.httpSource("dataSender").postData(data);
		assertValid(data.toUpperCase(), sinks.file());
	}

}
