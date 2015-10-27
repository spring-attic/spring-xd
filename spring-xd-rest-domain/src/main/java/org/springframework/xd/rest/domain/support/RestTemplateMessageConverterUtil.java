/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.xd.rest.domain.support;

import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

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
 * {@link org.springframework.xd.rest.domain.StreamDefinitionResource.Page}.
 * </p>
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
public class RestTemplateMessageConverterUtil {

	private static final boolean jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
			RestTemplateMessageConverterUtil.class.getClassLoader())
			&& ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					RestTemplateMessageConverterUtil.class.getClassLoader());

	/**
	 * Install message converters we're interested in, with json coming before xml.
	 */
	public static List<HttpMessageConverter<?>> installMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());
		if (jackson2Present) {
			messageConverters.add(new MappingJackson2HttpMessageConverter());
		}
		return messageConverters;
	}

}
