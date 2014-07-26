/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * {@link ErrorController} that causes 404 errors to be redirected to the Admin-UI 404 page.
 *
 * @author Gunnar Hillert
 */
@Controller
public class XdErrorController extends BasicErrorController {

	public XdErrorController() {
		super(new DefaultErrorAttributes());
	}

	@Override
	@RequestMapping(value = "${error.path:/error}", produces = "text/html")
	public ModelAndView errorHtml(HttpServletRequest request) {

		Integer status = (Integer) new ServletRequestAttributes(request).getAttribute(
				"javax.servlet.error.status_code", RequestAttributes.SCOPE_REQUEST);

		// TODO: better to toss this class completely and use an error page for 404
		switch (status) {
			case 404:
				return new ModelAndView(new RedirectView("/" + ConfigLocations.XD_ADMIN_UI_BASE_PATH + "/404.html"));
		}
		return super.errorHtml(request);
	}
}
