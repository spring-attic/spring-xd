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

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;


/**
 * Tests related to custom module packaging.
 * 
 * @author Eric Bottard
 */
public class ModuleClasspathTests extends AbstractStreamIntegrationTest {

	@Test
	public void testModuleWithClasspathAfterServerStarted() throws Exception {
		installTestModule("source", "time2");
		FileSink fileSink = newFileSink();
		stream().create(generateStreamName(), "time2 --fixedDelay=1000 | %s", fileSink);

		assertThat(fileSink, eventually(hasContentsThat(not(isEmptyOrNullString()))));
	}

	/**
	 * A workaround for Windows. Once the jar is loaded it cannot be deleted.
	 * 
	 * @throws IOException
	 */

	@Before
	public void cleanTestFiles() throws IOException {
		File testModuleDir = new File("../modules/source/time2");
		if (testModuleDir.exists()) {
			FileUtils.deleteDirectory(testModuleDir);
		}
	}
}
