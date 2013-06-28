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

import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.dsl.StreamConfigParser;
import org.springframework.xd.dirt.stream.dsl.ast.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamsNode;

/**
 * @author Andy Clement
 * @author Gunnar Hillert
 * @since 1.0
 */
public class EnhancedStreamParser implements StreamParser {

	@Override
	public List<ModuleDeploymentRequest> parse(String name, String config) {

		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(name, config);
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();

		List<ModuleNode> moduleNodes = ast.getModuleNodes();
		for (int m=moduleNodes.size()-1;m>=0;m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup(name);
			request.setType(determineType(m, moduleNodes.size(), moduleNode.getName()));
			request.setModule(moduleNode.getName());
			request.setIndex(m);
			if (moduleNode.hasArguments()) {
				ArgumentNode[] arguments = moduleNode.getArguments();
				for (int a=0;a<arguments.length;a++) {
					request.setParameter(arguments[a].getName(),arguments[a].getValue());
				}
			}
			requests.add(request);
		}
		return requests;
	}

	private String determineType(int m, int moduleNodesSize, String moduleName) {

		final String type;

		if (moduleNodesSize == 1) {
			if ("trigger".equalsIgnoreCase(moduleName)) {
				type = "trigger";
			}
			else {
				type = "job";
			}
		}
		else {
			type = "source";
		}

		return (m == 0) ? type : (m == moduleNodesSize - 1) ? "sink" : "processor";
	}

}
