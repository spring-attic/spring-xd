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
package org.springframework.xd.jdbc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mminella
 */
public class NamedColumnJdbcItemReaderFactoryTests {

	private NamedColumnJdbcItemReaderFactory factory;

	@Before
	public void setUp() {
		factory = new NamedColumnJdbcItemReaderFactory();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoSqlOrNamesSet() throws Exception {
		factory.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullColumns() throws Exception {
		factory.setTableName("foo");
		factory.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullTable() throws Exception {
		factory.setColumnNames("foo, bar");
		factory.afterPropertiesSet();
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factory.isSingleton());
	}

	@Test
	public void testGetObjectType() {
		assertEquals(NamedColumnJdbcItemReader.class, factory.getObjectType());
	}
}
