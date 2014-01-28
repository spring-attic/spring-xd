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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;

/**
 * Tests for the trigger source module.
 * 
 * @author Florent Biville
 */
public class TriggerModulesTest extends AbstractStreamIntegrationTest {

	private FileSink binaryFileSink = newFileSink().binary(true);

	@Test
	public void receivesContentsInstantlyByDefault() {
		stream().create(generateStreamName(), "trigger --payload='Hello' | %s", binaryFileSink);

		assertThat(binaryFileSink, eventually(hasContentsThat(equalTo("Hello"))));
	}

	@Test
	public void doesNotReceiveAnyContentsForAFarFuture() {
		stream().create(
				generateStreamName(), "trigger --payload='Hello' --date='25/12/2032' --dateFormat='dd/MM/yyyy' | %s",
				binaryFileSink
				);

		assertThat(binaryFileSink, not(eventually(1, 300, hasContentsThat(equalTo("Hello")))));

	}
}
