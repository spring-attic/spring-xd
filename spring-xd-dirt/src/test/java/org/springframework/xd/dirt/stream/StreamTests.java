/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.AfterClass;
import org.junit.Test;

import org.springframework.xd.module.SimpleModule;

/**
 * @author David Turanski
 */
public class StreamTests extends AbstractStreamTests {

	private static String outputDir = System.getProperty("java.io.tmpdir");

	@Test
	public void testTap() throws InterruptedException {
		this.deployStream("test1", "time | log");
		this.deployStream("tap", "tap:test1 > file --dir=" + outputDir);
	}

	@Test
	public void testOutputType() throws InterruptedException, IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		this.deployStream(
				"test2",
				"time --outputType=application/json | transform --inputType=org.springframework.xd.tuple.Tuple --expression=payload | file --dir="
						+ outputDir);
		SimpleModule time = (SimpleModule) getDeployedModule("test2", 0);
		assertEquals("application/json", time.getProperties().get("outputType"));
	}

	@AfterClass
	public static void cleanUp() {
		File f1 = new File(outputDir + "/tap.out");
		f1.deleteOnExit();
		File f2 = new File(outputDir + "/test2.out");
		f2.deleteOnExit();
	}
}
