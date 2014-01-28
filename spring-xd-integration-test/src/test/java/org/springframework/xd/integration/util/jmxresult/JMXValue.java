/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.util.jmxresult;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * @author Glenn Renfro
 */
public class JMXValue {
	
	ArrayList<Module> modules = new ArrayList<Module>();
	@JsonAnySetter
	public void handleUnknown(String key, Object value) throws Exception{
		Module module = Module.generateModuleFromJackson(key,value);
		modules.add(module);
	}
	public ArrayList<Module> getModules() {
		return modules;
	}
	public void setModules(ArrayList<Module> modules) {
		this.modules = modules;
	}
	
	
}
