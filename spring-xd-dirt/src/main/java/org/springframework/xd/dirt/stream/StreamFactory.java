/*
 * Copyright 2014-2015 the original author or authors.
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
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Factory class that builds {@link Stream} instances.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamFactory {

	/**
	 * DSL parser.
	 */
	private final XDStreamParser parser;

	/**
	 * Construct a StreamFactory to create {@link Stream} domain model instances.
	 *
	 * @param streamDefinitionRepository     repository for stream definitions
	 * @param moduleRegistry                 registry for module definitions
	 * @param moduleOptionsMetadataResolver  resolver for module options metadata
	 */
	public StreamFactory(StreamDefinitionRepository streamDefinitionRepository,
			ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.parser = new XDStreamParser(streamDefinitionRepository, moduleRegistry,
				moduleOptionsMetadataResolver);
	}

	/**
	 * Create a new instance of {@link Stream} for the given name and
	 * properties. The properties should at minimum contain the following
	 * entries:
	 * <table>
	 *   <tr>
	 *     <th>definition</th>
	 *     <td>DSL definition for stream</td>
	 *   </tr>
	 *   <tr>
	 *     <th>deploymentProperties</th>
	 *     <td>Deployment properties for stream</td>
	 *   </tr>
	 * </table>
	 *
	 * @param name        stream name
	 * @param properties  properties for stream
	 * @return new {@code Stream} domain model instance
	 */
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
				.setDeploymentProperties(DeploymentPropertiesFormat.parseDeploymentProperties(properties.get("deploymentProperties")))
				.setModuleDescriptors(descriptors).build();
	}

}
