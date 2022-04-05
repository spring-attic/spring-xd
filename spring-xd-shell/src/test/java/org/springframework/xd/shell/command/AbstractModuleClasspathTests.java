/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import org.junit.Test;

import org.springframework.xd.test.fixtures.FileSink;


/**
 * Tests related to custom module packaging.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class AbstractModuleClasspathTests extends AbstractStreamIntegrationTest {

	@Test
	public void testModuleWithClasspathAfterServerStarted() throws Exception {
		FileSink fileSink = newFileSink();
		stream().create(generateStreamName(), "time2 | %s", fileSink);
		assertThat(fileSink, eventually(hasContentsThat(not(isEmptyOrNullString()))));
	}

	@Test
	public void testUberJarModule() {
		FileSink fileSink = newFileSink();
		stream().create(generateStreamName(), "time3 | %s", fileSink);
		assertThat(fileSink, eventually(hasContentsThat(not(isEmptyOrNullString()))));
	}

	@Test
	public void testJavaConfigModule() {
		FileSink fileSink = newFileSink();
		stream().create(generateStreamName(), "time | siDslModule --prefix=foo --suffix=bar| %s", fileSink);
		assertThat(fileSink,
				eventually(allOf(hasContentsThat(containsString("foo")), hasContentsThat(containsString("bar")))));
	}
}
