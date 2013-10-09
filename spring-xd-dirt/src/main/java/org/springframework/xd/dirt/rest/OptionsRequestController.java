/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.DispatcherServlet;


/**
 * A controller that accepts any request, granted that it is a OPTIONS request.
 * 
 * <p>
 * Note that this controller will actually not be invoked, as {@link AccessControlInterceptor} will interrupt request
 * handling. It solely exists so that {@link DispatcherServlet} lets OPTIONS requests thru.
 * </p>
 * 
 * @author Eric Bottard
 */
@Controller
public class OptionsRequestController {

	@RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
	public void foo() {
		// Do nothing, as the request will have been intercepted by AccessControllerInterceptor anyway.
	}

}
