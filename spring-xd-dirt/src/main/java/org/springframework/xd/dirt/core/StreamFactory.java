/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamFactory {

	private final XDStreamParser parser;

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	public StreamFactory(ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
	}

	public Stream createStream(String name, Map<String, String> properties) {
		Assert.hasText(name, "Stream name is required");
		Assert.notNull(properties, "Stream properties are required");

		String definition = properties.get("definition");
		Assert.hasText(definition, "Stream deployment manifest requires a 'definition' property");

		String[] tokens = definition.split("\\|");
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();
		for (ModuleDeploymentRequest request : this.parser.parse(name, definition, ParsingContext.stream)) {
			requests.add(0, request);
		}

		Stream.Builder builder = new Stream.Builder();
		builder.setName(name);
		if (properties != null) {
			builder.setProperties(properties);
		}

		// TODO: compare with the createStream method of the prototype
		for (int i = 0; i < requests.size(); i++) {
			ModuleDeploymentRequest request = requests.get(i);
			String moduleName = request.getModule();
			String label;
			String rawModuleDefinition = tokens[i];
			// todo: this is a hack
			// we need to be able to determine each module and any source/sink channel
			// elements via the StreamDefinition once the parser returns that
			if (i == requests.size() - 1 && rawModuleDefinition.contains(">")) {
				if (ModuleType.sink == request.getType()) {
					rawModuleDefinition = rawModuleDefinition.split(">")[1].trim();
				}
				else {
					rawModuleDefinition = rawModuleDefinition.split(">")[0].trim();
				}
			}
			else if (i == 0 && rawModuleDefinition.contains(">")) {
				rawModuleDefinition = rawModuleDefinition.split(">")[0].trim();
			}
			if (rawModuleDefinition.contains(": ")) {
				String[] split = rawModuleDefinition.split("\\: ");
				label = split[0].trim();
			}
			else {
				label = String.format("%s-%d", moduleName, request.getIndex());
			}
			String sourceChannelName = request.getSourceChannelName();
			if (sourceChannelName != null) {
				builder.setSourceChannelName(sourceChannelName);
			}
			String sinkChannelName = request.getSinkChannelName();
			if (sinkChannelName != null) {
				builder.setSinkChannelName(sinkChannelName);
			}
			ModuleDefinition moduleDefinition = moduleDefinitionRepository.findByNameAndType(moduleName,
					request.getType());
			builder.addModuleDefinition(label, moduleDefinition, request.getParameters());
		}
		return builder.build();
	}

}
