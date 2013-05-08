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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 */
public class DefaultStreamParser implements StreamParser {

	@Override
	public List<ModuleDeploymentRequest> parse(String name, String config) {
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();
		String[] modules = StringUtils.tokenizeToStringArray(config, "|");
		Assert.isTrue(modules.length > 1, "at least 2 modules required");
		for (int i = modules.length - 1; i >= 0; i--) {
			String module = modules[i];
			if (i == 0 && module.trim().matches("^tap\\s*@.*")) {
				String streamToTap = module.substring(module.indexOf('@') + 1).trim();
				Assert.hasText(streamToTap, "tap requires a stream name after '@' char");
				module = "tap --channel=" + streamToTap + ".0";
			}
			String[] tokens = StringUtils.tokenizeToStringArray(module, " ");
			Assert.isTrue(tokens.length > 0, "no module name provided");
			String moduleName = tokens[0];
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup(name);
			request.setType((i == 0) ? "source" : (i == modules.length - 1) ? "sink" : "processor");
			request.setModule(moduleName);
			request.setIndex(i);
			if (tokens.length > 1) {
				for (int j = 1; j < tokens.length; j++) {
					String param = tokens[j];
					int splitIndex = param.indexOf('=');
					Assert.isTrue(splitIndex != -1 && param.startsWith("--"),
							"expected syntax '--key=value' for token following module name");
					String paramName = param.substring(2, splitIndex);
					String paramValue = param.substring(splitIndex + 1);
					request.setParameter(paramName, paramValue);
				}
			}
			requests.add(request);
		}
		return requests;
	}

}
