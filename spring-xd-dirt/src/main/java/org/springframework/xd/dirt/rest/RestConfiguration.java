/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.xd.dirt.rest;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import java.util.List;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import org.springframework.xd.dirt.plugins.job.support.ExecutionContextJacksonMixIn;
import org.springframework.xd.dirt.plugins.job.support.StepExecutionJacksonMixIn;
import org.springframework.xd.rest.domain.support.RestTemplateMessageConverterUtil;
import org.springframework.xd.rest.domain.util.ISO8601DateFormatWithMilliSeconds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Takes care of infrastructure setup for the web/rest layer.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Andrew Eisenberg
 * @author Scott Andrews
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@Configuration
@EnableHypermediaSupport(type = HAL)
@EnableSpringDataWebSupport
@ComponentScan(excludeFilters = @Filter(Configuration.class) )
public class RestConfiguration {

	@Bean
	public WebContentInterceptor webContentInterceptor() {
		WebContentInterceptor webContentInterceptor = new WebContentInterceptor();
		webContentInterceptor.setCacheSeconds(0);
		return webContentInterceptor;
	}

	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurerAdapter() {

			@Value("${xd.ui.allow_origin:http://localhost:9889}")
			private String allowedOrigin;

			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				RestTemplateMessageConverterUtil.installMessageConverters(converters);

				for (HttpMessageConverter<?> httpMessageConverter : converters) {
					if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
						final MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) httpMessageConverter;

						final ObjectMapper objectMapper = converter.getObjectMapper();
						objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
						objectMapper.setDateFormat(new ISO8601DateFormatWithMilliSeconds());
						objectMapper.addMixInAnnotations(StepExecution.class, StepExecutionJacksonMixIn.class);
						objectMapper.addMixInAnnotations(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
					}
				}

				converters.add(new ResourceHttpMessageConverter());
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(new AccessControlInterceptor(allowedOrigin));
				registry.addInterceptor(webContentInterceptor());
			}

		};
	}
}
