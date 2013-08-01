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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.xd.rest.client.util.RestTemplateMessageConverterUtil;

/**
 * Takes care of infrastructure setup for the web/rest layer.
 * 
 * @author Eric Bottard
 * @author David Turanski
 * @author Andrew Eisenberg
 */
@Configuration
@EnableWebMvc
@EnableHypermediaSupport
@EnableSpringDataWebSupport
@ComponentScan(excludeFilters = @Filter(Configuration.class))
public class RestConfiguration {
	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurerAdapter() {
			// TODO Access-Control-Allow-Origin header should not be hard-coded
			private static final String ALLOWED_ORIGIN = "http://localhost:9889";

			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				RestTemplateMessageConverterUtil.installMessageConverters(converters);
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(new HandlerInterceptorAdapter() {
					@Override
					public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
							throws Exception {

						// ensure CORS headers are added to every web response
						if (!response.containsHeader("Access-Control-Allow-Origin")) {
							response.addHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
						}
						if (!response.containsHeader("Access-Control-Allow-Methods")) {
							response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
						}
						return true;
					}
				});
			}

		};
	}

}
