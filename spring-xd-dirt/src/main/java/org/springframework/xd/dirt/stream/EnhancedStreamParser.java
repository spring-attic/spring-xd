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
import org.springframework.xd.dirt.stream.dsl.*;

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
		StreamNode stream = ast.getStreams().get(0);
		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		SinkChannelNode sinkChannel = stream.getSinkChannelNode();

		if (sourceChannel != null) {
			requests.get(requests.size() - 1).setSourceChannelName(sourceChannel.getChannelNode().getChannelName());
		}

		if (sinkChannel != null) {
			requests.get(0).setSinkChannelName(sinkChannel.getChannelNode().getChannelName());
		}

		for (int m = 0; m < moduleNodes.size(); m++) {
			ModuleDeploymentRequest request =  requests.get(m);
			request.setType(determineType(request, requests.size() - 1));
		}

		return requests;
	}

	private String determineType(ModuleDeploymentRequest request, int lastIndex) {
		int index = request.getIndex();
		if (request.getSourceChannelName() != null) {
			if (index == lastIndex) {
				return "sink";
			} else {
				return "processor";
			}
		}
		else if (request.getSinkChannelName() != null) {
			if (index == 0) {
				return "source";
			} else {
				return "processor";
			}
		}
		else if (lastIndex == 0) {
			// Only one module and no source or sink channels
			if ("trigger".equalsIgnoreCase(request.getModule())) {
				return "trigger";
			}
			else {
				return "job";
			}
		}
		else if (index == 0) {
			return "source";
		}
		else if (index == lastIndex) {
			return "sink";
		}
		return "processor";
	}
}

