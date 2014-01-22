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

package org.springframework.xd.yarn.app.xd.appmaster;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.yarn.am.monitor.ContainerMonitor;
import org.springframework.yarn.am.monitor.DefaultContainerMonitor;

/**
 * Controller which is used to interact with with an XD's {@code XdYarnAppmaster} to query a current status of it and
 * its containers.
 * 
 * @author Janne Valkealahti
 * 
 */
@Controller
public class StatusController {

	// TODO: for now just a placeholder

	@Autowired
	private XdYarnAppmaster appmaster;

	@RequestMapping("/yarn")
	public @ResponseBody
	String status() {
		ContainerMonitor monitor = appmaster.getMonitor();
		String msg = "";
		if (monitor instanceof DefaultContainerMonitor) {
			msg = ((DefaultContainerMonitor) monitor).toDebugString();
		}
		return "hello " + msg;
	}

}
