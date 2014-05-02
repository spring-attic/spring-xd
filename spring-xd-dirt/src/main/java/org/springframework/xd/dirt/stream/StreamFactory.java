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

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
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

		Deque<ModuleDescriptor> descriptors = new LinkedList<ModuleDescriptor>();
		for (ModuleDescriptor request : parser.parse(name, definition, ParsingContext.stream)) {
			descriptors.addFirst(request);
		}

		return new Stream.Builder()
				.setName(name)
				.setDeploymentProperties(parseDeploymentProperties(properties.get("deploymentProperties")))
				.setModuleDescriptors(descriptors).build();
	}

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key has the format:
	 * {@code module.[modulename].[key]}. Values may themselves contain commas, since the split points will be based
	 * upon the key pattern.
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
