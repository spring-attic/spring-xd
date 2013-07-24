/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.xd.module;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;

/**
 * @author David Turanski
 *
 */
public class SimpleModuleTests {
	@Test
	public void testGetAcceptedMediaTypes() {
		SimpleModule transformer = new SimpleModule("transformer", "processor");
		transformer.addComponents(new ClassPathResource("/org/springframework/xd/module/accepts-json.xml"));
		List<MediaType> mediaTypes = transformer.getAcceptedMediaTypes();
		assertEquals(1, mediaTypes.size());
		assertEquals(MediaType.APPLICATION_JSON, mediaTypes.get(0));
	}

	@Test
	public void testAcceptsAllMediaTypesByDefault() {
		SimpleModule transformer = new SimpleModule("transformer", "processor");
		transformer.addComponents(new ClassPathResource("/org/springframework/xd/module/default-accepts-all.xml"));
		List<MediaType> mediaTypes = transformer.getAcceptedMediaTypes();
		assertEquals(1, mediaTypes.size());
		assertEquals(MediaType.ALL, mediaTypes.get(0));
	}

	@Test
	public void testEmptyMediaTypesAcceptsAll() {
		SimpleModule transformer = new SimpleModule("transformer", "processor");
		transformer.addComponents(new ClassPathResource("/org/springframework/xd/module/empty-accepts-all.xml"));
		List<MediaType> mediaTypes = transformer.getAcceptedMediaTypes();
		assertEquals(1, mediaTypes.size());
		assertEquals(MediaType.ALL, mediaTypes.get(0));
	}

	@Test
	public void testAcceptsJavaObject() {
		SimpleModule transformer = new SimpleModule("transformer", "processor");
		transformer.addComponents(new ClassPathResource("/org/springframework/xd/module/accepts-java-object.xml"));
		List<MediaType> mediaTypes = transformer.getAcceptedMediaTypes();
		assertEquals(1, mediaTypes.size());
		assertEquals("x-java-object", mediaTypes.get(0).getSubtype());
		assertEquals("foo.Bar",mediaTypes.get(0).getParameter("type"));
	}

}
