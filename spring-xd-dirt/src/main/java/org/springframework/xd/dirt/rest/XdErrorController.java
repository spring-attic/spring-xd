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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.web.BasicErrorController;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * {@link ErrorController} that causes 404 errors to be redirected to the Admin-UI 404 page.
 *
 * @author Gunnar Hillert
 */
@Controller
public class XdErrorController extends BasicErrorController {

	@Override
	@RequestMapping(value = "${error.path:/error}", produces = "text/html")
	public ModelAndView errorHtml(HttpServletRequest request) {
		Map<String, Object> map = extract(new ServletRequestAttributes(request), false,
				false);

		int status = (int) map.get("status");

		switch (status) {
			case 404:
				return new ModelAndView(new RedirectView("/admin-ui/404.html"));
		}
		return super.errorHtml(request);
	}
}
