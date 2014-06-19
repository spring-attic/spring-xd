/*
 * Copyright 2014 the original author or authors.
 *
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

package org.springframework.xd.dirt.stream.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Parse streams and verify either the correct abstract syntax tree is produced or the current exception comes out.
 *
 * @author Gunnar Hillert
 */
public class StreamConfigParserTests {

	@Test
	public void testWithNullInputText() {
		try {
			StreamUtils.maskPasswordsInStreamDefinition(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("streamDefinition must be neither empty nor null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testWithEmptyInputText() {
		try {
			StreamUtils.maskPasswordsInStreamDefinition(" ");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("streamDefinition must be neither empty nor null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testProcessingOfNormalString() {
		final String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("new foo bar");
		Assert.assertEquals("new foo bar", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter1() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("mystream --password=12345");
		Assert.assertEquals("mystream --password=*****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter2() {
		final String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("filejdbc --driverClassName=org.postgresql.Driver --password=12345678");
		Assert.assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=********", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOneUppercasePasswordParameter() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("mystream --PASSWORD=12345");
		Assert.assertEquals("mystream --PASSWORD=*****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithPasswordParameterInMiddle() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("filejdbc --password=12345678 --driverClassName=org.postgresql.Driver");
		Assert.assertEquals("filejdbc --password=******** --driverClassName=org.postgresql.Driver", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithMultiplePasswordParametersAndMixedCase() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("mystream -- password=12 --PASSword=  1234 --PASSWORD=  1234");
		Assert.assertEquals("mystream -- password=** --PASSword=  **** --PASSWORD=  ****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithUnicodePasswordParameter() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("mystream --password=\u0411\u0435\u0440\u043b\u0438\u043d");
		Assert.assertEquals("mystream --password=******", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithPasswordParameterContainingADot() {
		String maskedPassword = StreamUtils.maskPasswordsInStreamDefinition("mystream -- password=   12.34.5");
		Assert.assertEquals("mystream -- password=   *******", maskedPassword);
	}

}
