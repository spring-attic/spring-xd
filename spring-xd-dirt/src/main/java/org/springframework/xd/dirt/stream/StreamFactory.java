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
import java.util.LinkedList;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.util.DeploymentPropertiesUtility;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Factory class that builds {@link Stream} using properties.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamFactory {

	private final XDStreamParser parser;

	public StreamFactory(StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
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
				.setDeploymentProperties(DeploymentPropertiesUtility.parseDeploymentProperties(properties.get("deploymentProperties")))
				.setModuleDescriptors(descriptors).build();
	}

}
