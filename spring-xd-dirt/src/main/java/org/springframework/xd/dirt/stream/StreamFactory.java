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

package org.springframework.xd.dirt.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamFactory {

	/**
	 * Pattern used for parsing a String of comma-delimited key=value pairs.
	 */
	private static final Pattern DEPLOYMENT_PROPERTY_PATTERN = Pattern.compile(",\\s*module\\.[^\\.]+\\.[^=]+=");

	private final XDStreamParser parser;

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	public StreamFactory(StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.parser = new XDStreamParser(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
	}

	public Stream createStream(String name, Map<String, String> properties) {
		Assert.hasText(name, "Stream name is required");
		Assert.notNull(properties, "Stream properties are required");

		String definition = properties.get("definition");
		Assert.hasText(definition, "Stream properties requires a 'definition' property");

		String[] tokens = definition.split("\\|");
		List<ModuleDeploymentRequest> requests = new ArrayList<ModuleDeploymentRequest>();
		for (ModuleDeploymentRequest request : this.parser.parse(name, definition, ParsingContext.stream)) {
			requests.add(0, request);
		}

		Stream.Builder builder = new Stream.Builder();
		builder.setName(name);
		builder.setDeploymentProperties(parseDeploymentProperties(properties.get("deploymentProperties")));

		// TODO: compare with the createStream method of the prototype
		for (int i = 0; i < requests.size(); i++) {
			ModuleDeploymentRequest request = requests.get(i);
			String moduleName = request.getModuleName();
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

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key has the format:
	 * {@code module.[modulename].[key]}. Values may themselves contain commas, since the split points will
	 * be based upon the key pattern.
	 *
	 * @param s the string to parse
	 * @return the Map of parsed key value pairs
	 */
	private static Map<String, String> parseDeploymentProperties(String s) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		if (!StringUtils.isEmpty(s)) {
			Matcher matcher = DEPLOYMENT_PROPERTY_PATTERN.matcher(s);
			int start = 0;
			while (matcher.find()) {
				addKeyValuePairAsProperty(s.substring(start, matcher.start()), deploymentProperties);
				start = matcher.start() + 1;
			}
			addKeyValuePairAsProperty(s.substring(start), deploymentProperties);
		}
		return deploymentProperties;
	}

	/**
	 * Adds a String of format key=value to the provided Map as a key/value pair.
	 *
	 * @param pair the String representation
	 * @param properties the Map to which the key/value pair should be added
	 */
	private static void addKeyValuePairAsProperty(String pair, Map<String, String> properties) {
		int firstEquals = pair.indexOf('=');
		if (firstEquals != -1) {
			// todo: should key only be a "flag" as in: put(key, true)?
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}

}
