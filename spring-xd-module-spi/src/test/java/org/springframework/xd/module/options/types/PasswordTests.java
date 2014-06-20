/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.module.options.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Contains some basic tests for the {@link Password} module option type.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class PasswordTests {

	/**
	 * Test method for {@link Password#getPassword()}.
	 */
	@Test
	public void testGetPassword() {
		final Password password = new Password("secret");
		assertEquals("secret", password.getPassword());
	}

	/**
	 * Test method for {@link Password#toString()}.
	 */
	@Test
	public void testToString() {
		final Password password = new Password("secret");
		assertEquals("secret", password.toString());
	}

}
