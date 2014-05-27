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

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.xd.module.ModuleDescriptor;

/**
 * Domain model for runtime Stream modules. A stream consists of a set of modules used
 * to process the flow of data.
 *
 * @author Patrick Peralta
 */
public class Stream {

	/**
	 * Name of stream.
	 */
	private final String name;

	/**
	 * Ordered list of {@link ModuleDescriptor}s comprising this stream.
	 * The source is the first entry and the sink is the last entry.
	 */
	private final LinkedList<ModuleDescriptor> descriptors;

	/**
	 * Deployment properties for this stream.
	 */
	private final Map<String, String> deploymentProperties;

	/**
	 * Construct a Stream.
	 *
	 * @param name                 stream name
	 * @param descriptors          module descriptors defined by this stream in stream
	 *                             flow order (source is first, sink is last)
	 * @param deploymentProperties stream deployment properties
	 */
	private Stream(String name, LinkedList<ModuleDescriptor> descriptors, Map<String, String> deploymentProperties) {
		this.name = name;
		this.descriptors = descriptors;
		this.deploymentProperties = deploymentProperties;
	}

	/**
	 * Return the name of this stream.
	 *
	 * @return stream name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the ordered list of modules for this stream. Modules
	 * are maintained in stream flow order (source is first, sink is last).
	 *
	 * @return list of module descriptors for this stream definition
	 */
	public Deque<ModuleDescriptor> getDescriptors() {
		return descriptors;
	}

	/**
	 * Return the ordered list of modules for this stream as a {@link List}.
	 * This allows for retrieval of modules in the stream by index.
	 * Modules are maintained in stream flow order (source is first, sink is last).
	 *
	 * @return list of module descriptors for this stream definition
	 */
	public List<ModuleDescriptor> getDescriptorsAsList() {
		return descriptors;
	}

	/**
	 * Return an iterator that indicates the order of module deployments for this
	 * stream. The modules are returned in reverse order; i.e. the sink is returned
	 * first followed by the processors in reverse order followed by the
	 * source.
	 *
	 * @return iterator that iterates over the modules in deployment order
	 */
	public Iterator<ModuleDescriptor> getDeploymentOrderIterator() {
		return descriptors.descendingIterator();
	}

	/**
	 * Return the deployment properties for this stream.
	 *
	 * @return stream deployment properties
	 */
	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Stream{name='" + name + "'}";
	}

	public ModuleDescriptor getModuleDescriptor(String moduleLabel, String moduleType) {
		for (ModuleDescriptor descriptor : descriptors) {
			if (descriptor.getModuleLabel().equals(moduleLabel)
					&& descriptor.getType().toString().equals(moduleType)) {
				return descriptor;
			}
		}
		throw new IllegalStateException(); // todo
	}


	/**
	 * Builder object for {@link Stream} that supports fluent style configuration.
	 */
	public static class Builder {

		/**
		 * @see Stream#name
		 */
		private String name;

		/**
		 * @see Stream#deploymentProperties
		 */
		private Map<String, String> deploymentProperties = Collections.emptyMap();

		/**
		 * @see Stream#descriptors
		 */
		private LinkedList<ModuleDescriptor> moduleDescriptors = new LinkedList<ModuleDescriptor>();

		/**
		 * Set the stream name.
		 *
		 * @param name stream name
		 *
		 * @return this builder
		 */
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the module descriptors for this stream.
		 *
		 * @param descriptors list of module descriptors in stream
		 *                    flow order (source is first, sink is last)
		 * @return this builder
		 */
		public Builder setModuleDescriptors(Deque<ModuleDescriptor> descriptors) {
			this.moduleDescriptors.addAll(descriptors);
			return this;
		}

		/**
		 * Set the deployment properties for the stream.
		 *
		 * @param deploymentProperties stream deployment properties
		 *
		 * @return this builder
		 */
		public Builder setDeploymentProperties(Map<String, String> deploymentProperties) {
			this.deploymentProperties = deploymentProperties;
			return this;
		}

		/**
		 * Create a new instance of {@link Stream}.
		 *
		 * @return new Stream instance
		 */
		public Stream build() {
			return new Stream(name, moduleDescriptors, deploymentProperties);
		}

	}

}
