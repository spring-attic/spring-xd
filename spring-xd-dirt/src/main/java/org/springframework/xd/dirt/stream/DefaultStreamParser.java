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

package org.springframework.xd.dirt.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class DefaultStreamParser implements XDParser {

	@Override
	public List<ModuleDeploymentRequest> parse(String name, String config) {
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();
		String[] modules = StringUtils.tokenizeToStringArray(config, "|");
		Assert.isTrue(modules.length > 1, "at least 2 modules required");
		for (int i = modules.length - 1; i >= 0; i--) {
			String module = modules[i].trim();
			if (i == 0 && module.matches("^tap\\s*@.*")) {
				String streamToTap = module.substring(module.indexOf('@') + 1).trim();
				Assert.hasText(streamToTap, "tap requires a stream name after '@' char");
				module = "tap --channel=" + streamToTap + ".0";
			}

			String moduleName = null;
			Properties parameters = getParameters(module);
			if (!CollectionUtils.isEmpty(parameters)) {
				Assert.isTrue(module.indexOf(" ") > 0,
						"invalid module definition - no space before parameter assignment" + module);
				moduleName = module.substring(0, module.indexOf(" "));
			}
			else {
				moduleName = module;
			}
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup(name);
			request.setType((i == 0) ? "source" : (i == modules.length - 1) ? "sink" : "processor");
			request.setModule(moduleName);
			request.setIndex(i);
			if (!CollectionUtils.isEmpty(parameters)) {
				for (Entry<Object, Object> parameter : parameters.entrySet()) {
					request.setParameter((String) parameter.getKey(), (String) parameter.getValue());
				}
			}
			requests.add(request);
		}
		return requests;
	}

	Properties getParameters(String module) {
		module = module.trim();
		int indx = 0;
		int nextIndex = -1;
		Properties parameters = new Properties();
		while (indx >= 0) {
			indx = module.indexOf("--", indx);
			Assert.isTrue(indx != 0, "invalid module definition - missing module name : " + module);

			if (indx > 0) {
				indx = indx + 2;
				nextIndex = module.indexOf("--", indx);
				String parameter = (nextIndex < 0) ? module.substring(indx) : module.substring(indx, nextIndex - 1);
				int splitIndex = parameter.indexOf("=");
				Assert.isTrue(splitIndex != -1 && splitIndex != parameter.length() - 1,
						"expected syntax '--key=value' for token following module name");

				parameters.put(parameter.substring(0, splitIndex), parameter.substring(splitIndex + 1));
			}
		}

		return parameters;
	}

}
