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

package org.springframework.xd.dirt.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tests the functionality of {@link PasswordUtils#maskPasswordsInDefinition(String)}
 * ensuring that stream parameters named {@code password} or {@code passwd} are
 * sufficiently masked.
 *
 * @author Gunnar Hillert
 */
public class PasswordUtilsTests {

	@Test
	public void testWithNullInputText() {
		try {
			PasswordUtils.maskPasswordsInDefinition(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("definition must be neither empty nor null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testWithEmptyInputText() {
		try {
			PasswordUtils.maskPasswordsInDefinition(" ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("definition must be neither empty nor null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testProcessingOfNormalString() {
		final String maskedPassword = PasswordUtils.maskPasswordsInDefinition("new foo bar");
		assertEquals("new foo bar", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter1() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream --password=12345");
		assertEquals("mystream --password=*****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter2() {
		final String maskedPassword = PasswordUtils.maskPasswordsInDefinition("filejdbc --driverClassName=org.postgresql.Driver --password=12345678");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=********", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter3() {
		final String maskedPassword = PasswordUtils.maskPasswordsInDefinition("filejdbc --driverClassName=org.postgresql.Driver --passwd=12345678");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --passwd=********", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOneUppercasePasswordParameter() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream --PASSWORD=12345");
		assertEquals("mystream --PASSWORD=*****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithPasswordParameterInMiddle() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("filejdbc --password=12345678 --driverClassName=org.postgresql.Driver");
		assertEquals("filejdbc --password=******** --driverClassName=org.postgresql.Driver", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithMultiplePasswordParametersAndMixedCase() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream -- password=12 --PASSword=  1234 --PASSWORD=  1234");
		assertEquals("mystream -- password=** --PASSword=  **** --PASSWORD=  ****", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithUnicodePasswordParameter() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream --password=\u0411\u0435\u0440\u043b\u0438\u043d");
		assertEquals("mystream --password=******", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithPasswordParameterContainingADot() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream -- password=   12.34.5");
		assertEquals("mystream -- password=   *******", maskedPassword);
	}

}
