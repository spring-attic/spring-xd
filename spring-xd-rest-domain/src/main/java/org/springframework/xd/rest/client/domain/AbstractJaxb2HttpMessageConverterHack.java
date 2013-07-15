/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.rest.client.domain;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.springframework.hateoas.PagedResources;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.AbstractJaxb2HttpMessageConverter;
import org.springframework.util.ReflectionUtils;

/**
 * Workaround for the fact that we can't easily interact with {@link AbstractJaxb2HttpMessageConverter}'s jaxbContexts
 * and adding @XmlSeeAlso won't cut it because for example we can't modify {@link PagedResources}.
 * 
 * This is meant to be used both server side and client side.
 * 
 * @author Eric Bottard
 * @see https://jira.springsource.org/browse/SPR-10262#comment-91685
 */
public class AbstractJaxb2HttpMessageConverterHack {

	private AbstractJaxb2HttpMessageConverterHack() {

	}

	public static void populateJAXBContext(List<HttpMessageConverter<?>> converters) {
		final Class<?>[] ourClasses = new Class[] { PagedResources.class, StreamDefinitionResource.class,
				TapDefinitionResource.class };
		Map<Class<?>, JAXBContext> contexts = new ConcurrentHashMap<Class<?>, JAXBContext>();
		try {
			for (Class<?> clazz : ourClasses) {
				contexts.put(clazz, JAXBContext.newInstance(ourClasses));
			}
		}
		catch (JAXBException e) {
			throw new IllegalStateException(e);
		}

		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AbstractJaxb2HttpMessageConverter) {
				AbstractJaxb2HttpMessageConverter<?> jaxbConverter = (AbstractJaxb2HttpMessageConverter<?>) converter;
				Field field = ReflectionUtils.findField(AbstractJaxb2HttpMessageConverter.class, "jaxbContexts");
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, jaxbConverter, contexts);
			}
		}

	}

}
