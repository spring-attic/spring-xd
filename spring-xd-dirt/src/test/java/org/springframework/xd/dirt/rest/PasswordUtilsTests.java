/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.Properties;

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
		final String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=12345678");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=********", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOnePasswordParameter3() {
		final String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --passwd=12345678");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --passwd=********", maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithOneUppercasePasswordParameter() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream --PASSWORD=12345");
		assertEquals("mystream --PASSWORD=*****", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter in middle of stream) called "password" should have its value masked.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterInMiddle() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --password=12345678 --driverClassName=org.postgresql.Driver");
		assertEquals("filejdbc --password=******** --driverClassName=org.postgresql.Driver", maskedPassword);
	}

	/**
	 * The password parameter can be upper-case, lower-case or mixed-case.
	 */
	@Test
	public void testMaskStreamDefinitionWithMultiplePasswordParametersAndMixedCase() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"mystream -- password=12 --PASSword=  1234 --PASSWORD=  1234");
		assertEquals("mystream -- password=** --PASSword=  **** --PASSWORD=  ****", maskedPassword);
	}

	/**
	 * The password parameter value in Russian (UTF) should be masked also.
	 */
	@Test
	public void testMaskStreamDefinitionWithUnicodePasswordParameter() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"mystream --password=\u0411\u0435\u0440\u043b\u0438\u043d");
		assertEquals("mystream --password=******", maskedPassword);
	}

	/**
	 * Mask Stream Definition With Password Parameter Containing a Dot.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterContainingADot() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("mystream -- password=   12.34.5");
		assertEquals("mystream -- password=   *******", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "passwwd" should have its value masked.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEnd() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --passwd=12345678");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --passwd=********", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains an underscore.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEndContainingUnderscore() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=12345678_abcd");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=*************", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains a currency symbols.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEndContainingCurrencySymbol() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=12345678$a\u20acbc\u00a5d");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=***************", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value is wrapped in quotation marks.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEndWrappedInQuotes() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=\"abcd\"");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=\"****\"", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains spaces.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEndWrappedInQuotesContainingSpaces() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=\"ab  cd\"");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=\"******\"", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains dashes.
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterAtEndWrappedInQuotesContainingDashes() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --driverClassName=org.postgresql.Driver --password=\"ab---cd\"");
		assertEquals("filejdbc --driverClassName=org.postgresql.Driver --password=\"*******\"", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter in middle of stream) called "password" with value wrapped in quotes should have its value masked
	 */
	@Test
	public void testMaskStreamDefinitionWithPasswordParameterInMiddleWrappedInQuotes() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --password=\"12345678\" --driverClassName=\"org.postgresql.Driver\"");
		assertEquals("filejdbc --password=\"********\" --driverClassName=\"org.postgresql.Driver\"", maskedPassword);
	}

	/**
	 * A basic stream definition parameter (parameter in middle of stream) called "password" with value wrapped in quotes should have its value masked
	 */
	@Test
	public void testMaskStreamDefinitionWithMultiplePasswordParametersWrappedInQuotes() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition(
				"filejdbc --password=\"12345678\" --driverClassName=\"org.postgresql.Driver\" --passwd=\"12345678\"");
		assertEquals(
				"filejdbc --password=\"********\" --driverClassName=\"org.postgresql.Driver\" --passwd=\"********\"",
				maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithCustomNamedPasswordParameterPrefixOnly() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("time | cpp --mypassword=123 | null");
		assertEquals(
				"time | cpp --mypassword=*** | null",
				maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithCustomNamedPasswordParameterSuffixOnly() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("time | cpp --passwordmy=123 | null");
		assertEquals(
				"time | cpp --passwordmy=*** | null",
				maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithCustomNamedPasswordParameterSuffixOnly2() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("time | cpp --passwordmy    =123 | null");
		assertEquals(
				"time | cpp --passwordmy    =*** | null",
				maskedPassword);
	}

	@Test
	public void testMaskStreamDefinitionWithCustomNamedPasswordParameterPrefixAndSuffix() {
		String maskedPassword = PasswordUtils.maskPasswordsInDefinition("time | cpp --mypassworditis=123 | null");
		assertEquals(
				"time | cpp --mypassworditis=*** | null",
				maskedPassword);
	}

	@Test
	public void testMaskString() {
		String maskedPassword = PasswordUtils.maskString("12345");
		assertEquals("*****", maskedPassword);
	}

	@Test
	public void testMaskStringWithNullString() {
		try {
			PasswordUtils.maskString(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'stringToMask' must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

	@Test
	public void testMaskPropertiesIfNecessaryWithNullProperties() {
		try {
			PasswordUtils.maskPropertiesIfNecessary(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'properties' must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

	@Test
	public void testMaskPropertiesIfNecessary() {
		final Properties properties = new Properties();
		properties.setProperty("nothingtomask", "1234");
		properties.setProperty("mypassword", "1234");
		properties.setProperty("passwdisMINE", "1234");

		PasswordUtils.maskPropertiesIfNecessary(properties);

		assertEquals("1234", properties.getProperty("nothingtomask"));
		assertEquals("****", properties.getProperty("mypassword"));
		assertEquals("****", properties.getProperty("passwdisMINE"));
	}
}
