/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.rest.meta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.util.XdUtils;
import org.springframework.xd.rest.domain.meta.VersionResource;

/**
 * Controller to retrieve version control information.
 *
 * @author Gunnar Hillert
 *
 * @since 1.1
 */
@Controller
@RequestMapping("/meta/version")
@ExposesResourceFor(VersionResource.class)
public class VersionController {

	@Autowired
	private Environment environment;

	/**
	 * Return version control information.
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public VersionResource getVersion() {

		final String version = XdUtils.getSpringXdVersion();
		final VersionResource versionResource;

		if (StringUtils.hasText(version)) {
			versionResource = new VersionResource(version);
		}
		else {
			versionResource = new VersionResource();
		}

		return versionResource;
	}

}
