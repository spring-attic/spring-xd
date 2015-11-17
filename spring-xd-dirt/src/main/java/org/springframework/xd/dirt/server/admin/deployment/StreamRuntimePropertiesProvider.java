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

package org.springframework.xd.dirt.server.admin.deployment;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * {@link RuntimeModuleDeploymentPropertiesProvider} that provides the stream partition/short circuit
 * properties for the given {@link ModuleDescriptor}.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
public class StreamRuntimePropertiesProvider extends RuntimeModuleDeploymentPropertiesProvider {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(StreamRuntimePropertiesProvider.class);

	/**
	 * The stream to create properties for.
	 */
	private final Stream stream;

	/**
	 * Construct a {@code StreamPartitionPropertiesProvider} for
	 * a {@link org.springframework.xd.dirt.core.Stream}.
	 *
	 * @param stream stream to create partition properties for
	 */
	public StreamRuntimePropertiesProvider(Stream stream,
			ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> propertiesProvider) {
		super(propertiesProvider);
		this.stream = stream;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuntimeModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor moduleDescriptor) {
		List<ModuleDescriptor> streamModules = stream.getModuleDescriptors();
		RuntimeModuleDeploymentProperties properties = super.propertiesForDescriptor(moduleDescriptor);
		int moduleSequence = properties.getSequence();
		int moduleIndex = moduleDescriptor.getIndex();

		// not first or input channel is named
		if (moduleIndex > 0 || isNamedChannelInput(moduleDescriptor)) {
			properties.put("consumer." + BusProperties.SEQUENCE, String.valueOf(moduleSequence));
			properties.put("consumer." + BusProperties.COUNT, String.valueOf(properties.getCount()));

		}
		if (moduleIndex > 0) {
			ModuleDescriptor previous = streamModules.get(moduleIndex - 1);
			ModuleDeploymentProperties previousProperties = deploymentPropertiesProvider.propertiesForDescriptor(previous);
			if (hasPartitionKeyProperty(previousProperties)) {
				properties.put("consumer." + BusProperties.PARTITION_INDEX, String.valueOf(moduleSequence - 1));
			}
			String minPartitionCount = previousProperties.get("producer." + BusProperties.MIN_PARTITION_COUNT);
			if (minPartitionCount != null) {
				properties.put("consumer." + BusProperties.MIN_PARTITION_COUNT, minPartitionCount);
			}
		}
		// Not last
		if (moduleIndex + 1 < streamModules.size()) {
			ModuleDeploymentProperties nextProperties = deploymentPropertiesProvider.propertiesForDescriptor(streamModules.get(moduleIndex + 1));
			String count = nextProperties.get(ModuleDeploymentProperties.COUNT_KEY);
			if (count != null) {
				properties.put("producer." + BusProperties.NEXT_MODULE_COUNT, count);
			}
			String concurrency = nextProperties.get("consumer."+BusProperties.CONCURRENCY);
			if (concurrency != null) {
				properties.put("producer." + BusProperties.NEXT_MODULE_CONCURRENCY, concurrency);
			}
		}

		// Partitioning
		if (hasPartitionKeyProperty(properties)) {
			if (moduleIndex == streamModules.size() && !isNamedChannelOutput(moduleDescriptor)) {
				logger.warn("Module '{}' is a sink module which contains a property " +
								"of '{}' used for data partitioning; this feature is only " +
								"supported for modules that produce data", moduleDescriptor,
						"producer.partitionKeyExpression");
			}
		}
		else if (moduleIndex + 1 < streamModules.size()) {
			// check for direct binding if the module is neither last nor partitioned
			ModuleDeploymentProperties nextProperties =
					deploymentPropertiesProvider.propertiesForDescriptor(streamModules.get(moduleIndex + 1));
			/*
			 *  A direct binding is allowed if all of the following are true:
			 *  1. the user did not explicitly disallow direct binding
			 *  2. this module is not a partitioning producer
			 *  3. this module is not the last one in a stream
			 *  4. both this module and the next module have a count of 0
			 *  5. both this module and the next module have the same criteria (both can be null)
			 */
			String directBindingKey = "producer." + BusProperties.DIRECT_BINDING_ALLOWED;
			String directBindingValue = properties.get(directBindingKey);
			if (directBindingValue != null && !"false".equalsIgnoreCase(directBindingValue)) {
				logger.warn(
						"Only 'false' is allowed as an explicit value for the {} property,  but the value was: '{}'",
						directBindingKey, directBindingValue);
			}
			if (!"false".equalsIgnoreCase(properties.get(directBindingKey))) {
				if (properties.getCount() == 0 && nextProperties.getCount() == 0) {
					String criteria = properties.getCriteria();
					if ((criteria == null && nextProperties.getCriteria() == null)
							|| (criteria != null && criteria.equals(nextProperties.getCriteria()))) {
						properties.put(directBindingKey, Boolean.toString(true));
					}
				}
			}
		}
		return properties;
	}

	private boolean isNamedChannelInput(ModuleDescriptor moduleDescriptor) {
		String sourceChannelName = moduleDescriptor.getSourceChannelName();
		return sourceChannelName != null
				&& (sourceChannelName.startsWith("tap:")
					|| sourceChannelName.startsWith("topic:")
					|| sourceChannelName.startsWith("queue:"));
	}

	private boolean isNamedChannelOutput(ModuleDescriptor moduleDescriptor) {
		String sinkChannelName = moduleDescriptor.getSinkChannelName();
		return sinkChannelName != null
				&& (sinkChannelName.startsWith("topic:")
				|| sinkChannelName.startsWith("queue:"));
	}

	/**
	 * Return {@code true} if the provided properties include a property
	 * used to extract a partition key.
	 *
	 * @param properties properties to examine for a partition key property
	 * @return true if the properties contain a partition key property
	 */
	private boolean hasPartitionKeyProperty(ModuleDeploymentProperties properties) {
		return (properties.containsKey("producer.partitionKeyExpression")
				|| properties.containsKey("producer.partitionKeyExtractorClass"));
	}
}
