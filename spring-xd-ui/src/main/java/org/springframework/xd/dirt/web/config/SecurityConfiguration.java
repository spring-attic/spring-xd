/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;

/**
 * Setup Spring Security for development/testing purposes.
 *
 * @author Gunnar Hillert
 */
@Configuration()
@EnableWebMvcSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private ContentNegotiationStrategy contentNegotiationStrategy;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final RequestMatcher matcher = new MediaTypeRequestMatcher(contentNegotiationStrategy, MediaType.TEXT_HTML);
		final String loginPage = "/admin-ui/login";

		http.csrf().disable()
		.authorizeRequests()
		.antMatchers("/admin-ui/styles/**").permitAll()
		.antMatchers("/admin-ui/images/**").permitAll()
		.antMatchers("/admin-ui/fonts/**").permitAll()
		.anyRequest().authenticated()
		.and()
		.formLogin()
		.loginPage(loginPage).permitAll().loginProcessingUrl("/admin-ui/login")
		.permitAll()
		.and()
		.logout().logoutUrl("/admin-ui/logout")
		.permitAll()
		.and()
		.httpBasic()
		.and()
		.exceptionHandling()
		.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(loginPage), matcher);
	}
}
