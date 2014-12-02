/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.rest.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.admin.history.StepExecutionHistory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.impl.support.ExecutionContextJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.ExitStatusJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobExecutionJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobInstanceJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobParameterJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobParametersJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.StepExecutionHistoryJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.StepExecutionJacksonMixIn;
import org.springframework.xd.rest.domain.support.RestTemplateMessageConverterUtil;
import org.springframework.xd.rest.domain.util.ISO8601DateFormatWithMilliSeconds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Base class for sub-parts of the API, allows sharing configured objects like the {@link RestTemplate}.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 *
 */
class AbstractTemplate {

	/**
	 * A template used for http interaction.
	 */
	protected RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected Map<String, UriTemplate> resources = new HashMap<String, UriTemplate>();

	/**
	 * Copy constructor.
	 */
	AbstractTemplate(AbstractTemplate other) {
		this.restTemplate = other.restTemplate;
		this.resources = other.resources;
	}

	/**
	 * Basic constructor, used solely by entry point to the API, namely {@link SpringXDTemplate}.
	 */
	AbstractTemplate(ClientHttpRequestFactory factory) {
		restTemplate = new RestTemplate(factory);
		List<HttpMessageConverter<?>> converters = RestTemplateMessageConverterUtil.installMessageConverters(new ArrayList<HttpMessageConverter<?>>());

		for (HttpMessageConverter<?> httpMessageConverter : converters) {
			if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) httpMessageConverter;
				final ObjectMapper objectMapper = converter.getObjectMapper();
				objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
				objectMapper.setDateFormat(new ISO8601DateFormatWithMilliSeconds());

				objectMapper.addMixInAnnotations(JobExecution.class, JobExecutionJacksonMixIn.class);
				objectMapper.addMixInAnnotations(JobParameters.class, JobParametersJacksonMixIn.class);
				objectMapper.addMixInAnnotations(JobParameter.class, JobParameterJacksonMixIn.class);
				objectMapper.addMixInAnnotations(JobInstance.class, JobInstanceJacksonMixIn.class);
				objectMapper.addMixInAnnotations(StepExecution.class, StepExecutionJacksonMixIn.class);
				objectMapper.addMixInAnnotations(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
				objectMapper.addMixInAnnotations(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
				objectMapper.addMixInAnnotations(ExitStatus.class, ExitStatusJacksonMixIn.class);
			}
		}

		converters.add(new StringHttpMessageConverter());
		converters.add(0, new ResourceHttpMessageConverter());
		restTemplate.setMessageConverters(converters);
		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));
	}
}
