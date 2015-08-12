/*
 * Copyright 2014 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.xd.module.ModuleType.processor;

import java.io.File;
import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.HostNotWindowsRule;

/**
 * Test module commands that require deleting an uploaded module. Such tests fail on Windows due to OS file locking.
 *
 * @author David Turanski
 */
public class AbstractJarDeletingModuleCommandsTests extends AbstractStreamIntegrationTest {

	@ClassRule
	public static HostNotWindowsRule hostNotWindowsRule = new HostNotWindowsRule();

	@Test
	public void testDeleteUploadedModuleUsedByStream() throws IOException {
		File moduleSource = new File("src/test/resources/spring-xd/xd/modules/processor/siDslModule.jar");
		module().upload("siDslModule2", processor, moduleSource);
		stream().createDontDeploy("foo", "http | siDslModule2 --prefix=foo | log");
		assertFalse(module().delete("siDslModule2", processor));
	}

	//TODO: Investigate why this test fails on windows
	@Test
	public void testDeleteComposedModuleUsedByStream() throws Exception {
		module().compose("myhttp", "http | filter");
		stream().createDontDeploy("foo", "myhttp | log");
		assertFalse(module().delete("myhttp", ModuleType.source));
		// Now deleting blocking stream
		stream().destroyStream("foo");
		assertTrue(module().delete("myhttp", ModuleType.source));
	}
}
