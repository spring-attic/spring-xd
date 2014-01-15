/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.rest.client.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.springframework.hateoas.PagedResources;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.AbstractJaxb2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.RuntimeContainerInfoResource;
import org.springframework.xd.rest.client.domain.RuntimeModuleInfoResource;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;
import org.springframework.xd.rest.client.domain.XDRuntime;
import org.springframework.xd.rest.client.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.client.domain.metrics.CounterResource;
import org.springframework.xd.rest.client.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.client.domain.metrics.GaugeResource;
import org.springframework.xd.rest.client.domain.metrics.MetricResource;
import org.springframework.xd.rest.client.domain.metrics.RichGaugeResource;

/**
 * Utility class that does two things:
 * <ol>
 * <li>Resets a {@link RestTemplate}'s message converters list to have json support come <em>before</em> xml.</li>
 * <li>Force injects JAXBContexts that know about our particular classes</li>
 * </ol>
 * 
 * <p>
 * The second item is necessary when marshalling (on the server) instances of <i>e.g.</i> {@link PagedResources} because
 * of type erasure. This hack can be worked around when un-marshalling (on the client) with use of constructs like
 * {@link org.springframework.xd.rest.client.domain.StreamDefinitionResource.Page}.
 * </p>
 * 
 * @author Eric Bottard
 */
public class RestTemplateMessageConverterUtil {

	private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			RestTemplateMessageConverterUtil.class.getClassLoader());

	private static final boolean jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
			RestTemplateMessageConverterUtil.class.getClassLoader())
			&& ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					RestTemplateMessageConverterUtil.class.getClassLoader());

	private static final boolean jacksonPresent = ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper",
			RestTemplateMessageConverterUtil.class.getClassLoader())
			&& ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator",
					RestTemplateMessageConverterUtil.class.getClassLoader());

	private static final Class<?>[] ourClasses = { PagedResources.class, StreamDefinitionResource.class,
		JobDefinitionResource.class, ModuleDefinitionResource.class, RuntimeContainerInfoResource.class,
		RuntimeModuleInfoResource.class,
		MetricResource.class, GaugeResource.class,
		AggregateCountsResource.class, CounterResource.class, XDRuntime.class, FieldValueCounterResource.class,
		RichGaugeResource.class };

	private RestTemplateMessageConverterUtil() {

	}

	/**
	 * Install message converters we're interested in, with json coming before xml.
	 */
	@SuppressWarnings("deprecation")
	public static List<HttpMessageConverter<?>> installMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());
		if (jackson2Present) {
			messageConverters.add(new MappingJackson2HttpMessageConverter());
		}
		else if (jacksonPresent) {
			// avoiding import of MappingJacksonHttpMessageConverter to prevent deprecation warning
			messageConverters.add(new org.springframework.http.converter.json.MappingJacksonHttpMessageConverter());
		}
		if (jaxb2Present) {
			Jaxb2RootElementHttpMessageConverter jaxbConverter = new Jaxb2RootElementHttpMessageConverter();
			initializeJAXBContexts(jaxbConverter);
			messageConverters.add(jaxbConverter);
		}
		return messageConverters;
	}

	private static void initializeJAXBContexts(AbstractJaxb2HttpMessageConverter<?> c) {

		// Ugliest hack ever to workaround https://jira.springsource.org/browse/SPR-10262
		Field f = ReflectionUtils.findField(AbstractJaxb2HttpMessageConverter.class, "jaxbContexts");
		ReflectionUtils.makeAccessible(f);
		@SuppressWarnings("unchecked")
		Map<Class<?>, JAXBContext> contexts = (Map<Class<?>, JAXBContext>) ReflectionUtils.getField(f, c);
		try {
			for (Class<?> clazz : ourClasses) {
				JAXBContext context = JAXBContext.newInstance(ourClasses);
				contexts.put(clazz, context);
			}
		}
		catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
	}

}
