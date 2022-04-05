/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.dirt.web.controller.support.AuthenticationRequest;

/**
 * Controller responsible for handling web-logins as well as basic redirects for
 * for the admin-ui.
 *
 * @author Gunnar Hillert
 */
@Controller
public class LoginController {

	private final AuthenticationManager authenticationManager;

	@Autowired
	public LoginController(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Autowired
	ApplicationContext applicationContext;

	@RequestMapping(value = "/authenticate", method = { RequestMethod.POST })
	@ResponseBody
	public String authorize(
			@RequestBody AuthenticationRequest authenticationRequest,
			HttpServletRequest request) {

		final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				authenticationRequest.getUsername(), authenticationRequest.getPassword());
		final Authentication authentication = this.authenticationManager.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		final HttpSession session = request.getSession(true);
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				SecurityContextHolder.getContext());

		return session.getId();
	}

	@RequestMapping("/admin-ui/")
	String index() {
		return "forward:/admin-ui/index.html";
	}

	@RequestMapping("/admin-ui")
	String indexWithoutTrailingSlash() {
		return "redirect:/admin-ui/";
	}
}
