/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.xd.dirt.stream.StreamDeployer;

@Controller
@RequestMapping("/streams")
public class StreamsController {

	@Autowired
	private StreamDeployer streamDeployer;
	
	@RequestMapping(value="/{name}", method= {RequestMethod.PUT, RequestMethod.POST})
	public void deploy(@PathVariable("name") String name, @RequestBody String dsl) {
		streamDeployer.deployStream(name, dsl);
	}
	
	@RequestMapping(value="/{name}", method = RequestMethod.DELETE)
	public void deploy(@PathVariable("name") String name) {
		streamDeployer.undeployStream(name);
	}
	
	
}
