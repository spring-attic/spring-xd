/*
 * Copyright 2009-2013 the original author or authors.
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

package org.springframework.xd.shell.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Gunnar Hillert
 * @author Stephan Oudmaijer
 * 
 * @since 1.0
 * 
 */
public class CommonUtilsTest {

	@Test
	public void testValidEmailAddress() {
		assertTrue(CommonUtils.isValidEmail("test@cloudfoundry.com"));
	}

	@Test
	public void testInValidEmailAddress() {
		assertFalse(CommonUtils.isValidEmail("test123"));
	}

	@Test
	public void testPadRightWithNullString() {
		assertEquals("     ", CommonUtils.padRight(null, 5));
	}

	@Test
	public void testPadRightWithEmptyString() {
		assertEquals("     ", CommonUtils.padRight("", 5));
	}

	@Test
	public void testPadRight() {
		assertEquals("foo  ", CommonUtils.padRight("foo", 5));
	}

	@Test
	public void testMaskPassword() {
		assertEquals("******", CommonUtils.maskPassword("foobar"));
	}

}
