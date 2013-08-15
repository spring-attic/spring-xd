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

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.stream.dsl.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.SinkChannelNode;
import org.springframework.xd.dirt.stream.dsl.SourceChannelNode;
import org.springframework.xd.dirt.stream.dsl.StreamConfigParser;
import org.springframework.xd.dirt.stream.dsl.StreamNode;
import org.springframework.xd.dirt.stream.dsl.StreamsNode;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * @author Andy Clement
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @since 1.0
 */
public class EnhancedStreamParser implements XDParser {

	private CrudRepository<? extends BaseDefinition, String> repository;

	private ModuleRegistry moduleRegistry;

	public EnhancedStreamParser(CrudRepository<? extends BaseDefinition, String> repository,
			ModuleRegistry moduleRegistry) {
		Assert.notNull(repository, "repository can not be null");
		Assert.notNull(moduleRegistry, "moduleRegistry can not be null");

		this.repository = repository;
		this.moduleRegistry = moduleRegistry;
	}

	public EnhancedStreamParser(ModuleRegistry moduleRegistry) {
		// no repository, will not be able to resolve substream/label references
		Assert.notNull(moduleRegistry, "moduleRegistry can not be null");

		this.moduleRegistry = moduleRegistry;
	}

	@Override
	public List<ModuleDeploymentRequest> parse(String name, String config) {

		StreamConfigParser parser = new StreamConfigParser(repository);
		StreamsNode ast = parser.parse(name, config);
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();

		List<ModuleNode> moduleNodes = ast.getModuleNodes();
		for (int m = moduleNodes.size() - 1; m >= 0; m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup(name);
			request.setModule(moduleNode.getName());
			request.setIndex(m);
			if (moduleNode.hasArguments()) {
				ArgumentNode[] arguments = moduleNode.getArguments();
				for (int a = 0; a < arguments.length; a++) {
					request.setParameter(arguments[a].getName(), arguments[a].getValue());
				}
			}
			requests.add(request);
		}
		StreamNode stream = ast.getStreams().get(0);
		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		SinkChannelNode sinkChannel = stream.getSinkChannelNode();

		if (sourceChannel != null) {
			requests.get(requests.size() - 1).setSourceChannelName(sourceChannel.getChannelName());
		}

		if (sinkChannel != null) {
			requests.get(0).setSinkChannelName(sinkChannel.getChannelName());
		}

		for (int m = 0; m < moduleNodes.size(); m++) {
			ModuleDeploymentRequest request = requests.get(m);
			request.setType(determineType(request, requests.size() - 1).getTypeName());
		}

		return requests;
	}

	private ModuleType determineType(ModuleDeploymentRequest request, int lastIndex) {
		ModuleType moduleType = getNamedChannelModuleType(request, lastIndex);
		if (moduleType != null) {
			return moduleType;
		}
		String type = null;
		String name = request.getModule();
		int index = request.getIndex();
		List<ModuleDefinition> defs = moduleRegistry.findDefinitions(name);

		if (defs.size() == 0) {
			throw new RuntimeException("Module definition is missing for " + name);
		}
		if (defs.size() == 1) {
			type = defs.get(0).getType();
		}
		if (lastIndex == 0) {
			for (ModuleDefinition def : defs) {
				if (def.getType().equals(ModuleType.JOB.getTypeName())
						|| def.getType().equals(ModuleType.TRIGGER.getTypeName())) {
					type = def.getType();
				}
			}
		}
		else if (index == 0) {
			type = ModuleType.SOURCE.getTypeName();
		}
		else if (index == lastIndex) {
			type = ModuleType.SINK.getTypeName();
		}
		if (type == null) {
			throw new NoSuchModuleException(name);
		}
		return verifyModuleOfTypeExists(name, type);
	}

	private ModuleType getNamedChannelModuleType(ModuleDeploymentRequest request, int lastIndex) {
		String type = null;
		String moduleName = request.getModule();
		int index = request.getIndex();
		if (request.getSourceChannelName() != null) {
			if (index == lastIndex) {
				type = ModuleType.SINK.getTypeName();
			}
			else {
				type = ModuleType.PROCESSOR.getTypeName();
			}
		}
		else if (request.getSinkChannelName() != null) {
			if (index == 0) {
				type = ModuleType.SOURCE.getTypeName();
			}
			else {
				type = ModuleType.PROCESSOR.getTypeName();
			}
		}
		return (type == null) ? null : verifyModuleOfTypeExists(moduleName, type);
	}

	private ModuleType verifyModuleOfTypeExists(String moduleName, String type) {
		ModuleDefinition def = moduleRegistry.lookup(moduleName, type);
		if (def == null || def.getResource() == null) {
			throw new NoSuchModuleException(moduleName);
		}
		return ModuleType.getModuleTypeByTypeName(def.getType());
	}

}
