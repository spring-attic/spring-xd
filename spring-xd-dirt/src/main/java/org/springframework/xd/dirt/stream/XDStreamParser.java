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
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.module.CompositeModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;
import org.springframework.xd.dirt.stream.dsl.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.SinkChannelNode;
import org.springframework.xd.dirt.stream.dsl.SourceChannelNode;
import org.springframework.xd.dirt.stream.dsl.StreamConfigParser;
import org.springframework.xd.dirt.stream.dsl.StreamNode;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

/**
 * @author Andy Clement
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Mark Fisher
 * @since 1.0
 */
public class XDStreamParser implements XDParser {

	private CrudRepository<? extends BaseDefinition, String> repository;

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	public XDStreamParser(CrudRepository<? extends BaseDefinition, String> repository,
			ModuleDefinitionRepository moduleDefinitionRepository) {
		Assert.notNull(repository, "repository can not be null");
		Assert.notNull(moduleDefinitionRepository, "moduleDefinitionRepository can not be null");
		this.repository = repository;
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	public XDStreamParser(ModuleDefinitionRepository moduleDefinitionRepository) {
		// no crud repository, will not be able to resolve substream/label references
		Assert.notNull(moduleDefinitionRepository, "moduleDefinitionRepository can not be null");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	@Override
	public List<ModuleDeploymentRequest> parse(String name, String config) {

		StreamConfigParser parser = new StreamConfigParser(repository);
		StreamNode stream = parser.parse(name, config);
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();

		List<ModuleNode> moduleNodes = stream.getModuleNodes();
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
		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		SinkChannelNode sinkChannel = stream.getSinkChannelNode();

		if (sourceChannel != null) {
			requests.get(requests.size() - 1).setSourceChannelName(sourceChannel.getChannelName());
		}

		if (sinkChannel != null) {
			requests.get(0).setSinkChannelName(sinkChannel.getChannelName());
		}

		// Now that we know about source and sink channel names,
		// do a second pass to determine type. Also convert to composites.
		// And while we're at it (and type is known), validate module name and options
		List<ModuleDeploymentRequest> result = new ArrayList<ModuleDeploymentRequest>(requests.size());
		for (ModuleDeploymentRequest original : requests) {
			original.setType(determineType(original, requests.size() - 1));

			// definition is guaranteed to be non-null here
			ModuleDefinition moduleDefinition = moduleDefinitionRepository.findByNameAndType(original.getModule(),
					original.getType());
			ModuleOptionsMetadata optionsMetadata = moduleDefinition.getModuleOptionsMetadata();
			try {
				optionsMetadata.interpolate(original.getParameters());
			}
			catch (BindException e) {
				throw ModuleConfigurationException.fromBindException(original.getModule(), original.getType(), e);
			}

			result.add(convertToCompositeIfNecessary(original));
		}
		return result;
	}

	private ModuleType determineType(ModuleDeploymentRequest request, int lastIndex) {
		ModuleType moduleType = getNamedChannelModuleType(request, lastIndex);
		if (moduleType != null) {
			return moduleType;
		}
		ModuleType type = null;
		String name = request.getModule();
		int index = request.getIndex();
		List<ModuleDefinition> defs = moduleDefinitionRepository.findByName(name);
		if (defs.size() == 0) {
			throw new RuntimeException("Module definition is missing for " + name);
		}
		if (defs.size() == 1) {
			type = defs.get(0).getType();
		}
		if (lastIndex == 0) {
			for (ModuleDefinition def : defs) {
				if (def.getType() == ModuleType.job) {
					type = def.getType();
				}
			}
		}
		else if (index == 0) {
			type = ModuleType.source;
		}
		else if (index == lastIndex) {
			type = ModuleType.sink;
		}
		if (type == null) {
			throw new NoSuchModuleException(name);
		}
		return verifyModuleOfTypeExists(request, name, type);
	}

	private ModuleType getNamedChannelModuleType(ModuleDeploymentRequest request, int lastIndex) {
		ModuleType type = null;
		String moduleName = request.getModule();
		int index = request.getIndex();
		if (request.getSourceChannelName() != null) { // preceded by >, so not a source
			if (index == lastIndex) { // this is the final module of the stream
				if (request.getSinkChannelName() != null) { // but followed by >, so not a sink
					type = ModuleType.processor;
				}
				else { // final module and no >, so IS a sink
					type = ModuleType.sink;
				}
			}
			else { // not final module, must be a processor
				type = ModuleType.processor;
			}
		}
		else if (request.getSinkChannelName() != null) { // followed by >, so not a sink
			if (index == 0) { // first module in a stream, and not preceded by >, so IS a source
				type = ModuleType.source;
			}
			else { // not first module, and followed by >, so not a source or sink
				type = ModuleType.processor;
			}
		}
		return (type == null) ? null : verifyModuleOfTypeExists(request, moduleName, type);
	}

	private ModuleDeploymentRequest convertToCompositeIfNecessary(ModuleDeploymentRequest request) {
		ModuleDefinition def = moduleDefinitionRepository.findByNameAndType(request.getModule(), request.getType());
		if (def != null && def.getDefinition() != null) {
			List<ModuleDeploymentRequest> composedModuleRequests = parse(def.getName(), def.getDefinition());
			request = new CompositeModuleDeploymentRequest(request, composedModuleRequests);
		}
		return request;
	}

	private ModuleType verifyModuleOfTypeExists(ModuleDeploymentRequest request, String moduleName, ModuleType type) {
		ModuleDefinition def = moduleDefinitionRepository.findByNameAndType(moduleName, type);
		if (def == null || def.getResource() == null) {
			List<ModuleDefinition> definitions = moduleDefinitionRepository.findByName(moduleName);
			if (definitions.isEmpty()) {
				throw new NoSuchModuleException(moduleName);
			}
			// TODO: revisit this method altogether; it shouldn't apply to composite modules but at this stage we don't
			// know whether the 'stream' is actually a composite module (and we do want to verify complete streams)
			// the following method was removed from MDR: (leaving here as a reminder for the TODO)

			// The module is known but this doesn't seem to be a standard stream,
			// assume it is a composite module stream that isn't deployable by itself
			// request.tagAsUndeployable();
			return definitions.get(0).getType();
		}
		return def.getType();
	}

}
