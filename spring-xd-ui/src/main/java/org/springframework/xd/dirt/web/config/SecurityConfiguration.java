/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.web.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.HeaderHttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;

/**
 * Setup Spring Security for the http endpoints of the application.
 *
 * @author Gunnar Hillert
 * @author Eric Bottard
 */
@Configuration
@ConditionalOnProperty("security.basic.enabled")
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	public static final Pattern AUTHORIZATION_RULE;

	static {
		String methodsRegex = StringUtils.arrayToDelimitedString(HttpMethod.values(),
				"|");
		AUTHORIZATION_RULE = Pattern
				.compile("(" + methodsRegex + ")\\s+(.+)\\s+=>\\s+(.+)");
	}

	@Bean
	public SessionRepository<ExpiringSession> sessionRepository() {
		return new MapSessionRepository();
	}

	@Autowired
	private ContentNegotiationStrategy contentNegotiationStrategy;

	@Value("${security.basic.realm}")
	private String realm;

	@Autowired
	private AuthorizationConfig config;

	@Bean
	public AuthorizationConfig config() {
		return new AuthorizationConfig();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
				contentNegotiationStrategy,
				MediaType.TEXT_HTML);

		final String loginPage = "/admin-ui/#/login";

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(realm);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security = http
				.csrf()
				.disable()
				.authorizeRequests()
				.antMatchers("/")
				.authenticated()
				.antMatchers("/admin-ui/**").permitAll()
				.antMatchers("/authenticate").permitAll()
				.antMatchers("/security/info").permitAll()
				.antMatchers("/assets/**").permitAll();

		security = configureSimpleSecurity(security);

		security.and()
				.formLogin().loginPage(loginPage)
				.loginProcessingUrl("/admin-ui/login")
				.defaultSuccessUrl("/admin-ui/").permitAll()
				.and().logout().logoutUrl("/admin-ui/logout").logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
				.permitAll()
				.and().httpBasic()
				.and().exceptionHandling()
				.defaultAuthenticationEntryPointFor(
						new LoginUrlAuthenticationEntryPoint(loginPage),
						textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint,
						AnyRequestMatcher.INSTANCE);

		security.anyRequest().denyAll();

		final SessionRepositoryFilter<ExpiringSession> sessionRepositoryFilter = new SessionRepositoryFilter<ExpiringSession>(
				sessionRepository());
		sessionRepositoryFilter
				.setHttpSessionStrategy(new HeaderHttpSessionStrategy());

		http.addFilterBefore(sessionRepositoryFilter,
				ChannelProcessingFilter.class).csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
	}

	/**
	 * Read the configuration for "simple" (that is, not ACL based) security and apply it.
	 */
	private ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry configureSimpleSecurity(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security) {
		for (String rule : config.getRules()) {
			Matcher matcher = AUTHORIZATION_RULE.matcher(rule);
			Assert.isTrue(matcher.matches(),
					String.format(
							"Unable to parse security rule [%s], expected format is 'HTTP_METHOD ANT_PATTERN => SECURITY_ATTRIBUTE(S)'",
							rule));

			HttpMethod method = HttpMethod.valueOf(matcher.group(1));
			String urlPattern = matcher.group(2);
			String attribute = matcher.group(3);

			security = security.antMatchers(method, urlPattern).access(attribute);
		}
		return security;
	}

	/**
	 * Holds configuration for the authorization aspects of security.
	 *
	 * @author Eric Bottard
	 */
	@ConfigurationProperties(prefix = "xd.security.authorization")
	public static class AuthorizationConfig {

		private List<String> rules = new ArrayList<String>();

		public List<String> getRules() {
			return rules;
		}

		public void setRules(List<String> rules) {
			this.rules = rules;
		}
	}
}
