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

import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.xd.dirt.plugins.job.support.StepExecutionJacksonMixIn;
import org.springframework.xd.rest.client.util.RestTemplateMessageConverterUtil;

import com.mangofactory.swagger.configuration.DocumentationConfig;

/**
 * Takes care of infrastructure setup for the web/rest layer.
 * 
 * @author Eric Bottard
 * @author David Turanski
 * @author Andrew Eisenberg
 * @author Scott Andrews
 * @author Gunnar Hillert
 */
@Configuration
@EnableHypermediaSupport
@EnableSpringDataWebSupport
@Import(DocumentationConfig.class)
@PropertySource("classpath:swagger.properties")
@ComponentScan(excludeFilters = @Filter(Configuration.class))
public class RestConfiguration {

	@Value("${XD_HOME}")
	private String xdHome;

	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurerAdapter() {

			private String adminUIRoot;

			private String swaggerRoot;

			// N.B. must end in "/"
			@Value("${xd.ui.home:file:${XD_HOME}/spring-xd-ui}/")
			public void setAdminUIResourceRoot(String root) {
				this.adminUIRoot = root.endsWith("/") ? root : root + "/";
			}

			// N.B. must end in "/"
			@Value("${SWAGGER_ROOT:file:${XD_HOME}/swagger-ui/}")
			public void setSwaggerResourceRoot(String root) {
				this.swaggerRoot = root.endsWith("/") ? root : root + "/";
			}


			@Value("${xd.ui.allow_origin:http://localhost:9889}")
			private String allowedOrigin;

			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				RestTemplateMessageConverterUtil.installMessageConverters(converters);

				for (HttpMessageConverter<?> httpMessageConverter : converters) {
					if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
						final MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) httpMessageConverter;
						converter.getObjectMapper().addMixInAnnotations(StepExecution.class,
								StepExecutionJacksonMixIn.class);
					}
				}

				converters.add(new ResourceHttpMessageConverter());
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(new AccessControlInterceptor(allowedOrigin));
			}

			// add a static resource handler for the UI
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler("/admin-ui/**", "/admin-ui/").addResourceLocations(
						adminUIRoot);

				registry.addResourceHandler("/swagger/**", "/swagger").addResourceLocations(
						swaggerRoot);
			}

			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addViewController("admin-ui").setViewName("/admin-ui/index.html");
				registry.addViewController("swagger").setViewName("redirect:/swagger/index.html");
			}
		};
	}
}
