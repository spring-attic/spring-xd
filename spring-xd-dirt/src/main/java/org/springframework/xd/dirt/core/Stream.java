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
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

//todo: decide if a Stream really should provide ContainerMatcher (as done in the prototype)

/**
 * Domain model for an XD Stream. A stream consists of a set of modules used to process the flow of data.
 * 
 * @author Patrick Peralta
 */
public class Stream {

	/**
	 * Name of stream.
	 */
	private final String name;

	/**
	 * Stream definition.
	 */
	private final String definition;

	/**
	 * Source module for this stream. This module is responsible for obtaining data for this stream.
	 */
	private final ModuleDescriptor source;

	/**
	 * Source channel for this stream (only present if no source module).
	 */
	private final String sourceChannelName;

	/**
	 * Ordered list of processor modules. The data obtained by the source module will be fed to these processors in the
	 * order indicated by this list.
	 */
	private final List<ModuleDescriptor> processors;

	/**
	 * Sink module for this stream. This is the ultimate destination for the stream data.
	 */
	private final ModuleDescriptor sink;

	/**
	 * Sink channel for this stream (only present if no sink module).
	 */
	private final String sinkChannelName;

	/**
	 * Properties for this stream.
	 */
	private final Map<String, String> properties;

	/**
	 * Construct a Stream.
	 * 
	 * @param name stream name
	 * @param source source module
	 * @param sourceChannelName source channel
	 * @param processors processor modules
	 * @param sink sink module
	 * @param sinkChannelName sink channel
	 * @param properties stream properties
	 */
	private Stream(String name, ModuleDescriptor source, String sourceChannelName, List<ModuleDescriptor> processors,
			ModuleDescriptor sink, String sinkChannelName, Map<String, String> properties) {
		this.name = name;
		this.source = source;
		this.sourceChannelName = sourceChannelName;
		this.processors = new LinkedList<ModuleDescriptor>(processors == null
				? Collections.<ModuleDescriptor> emptyList()
				: processors);
		this.sink = sink;
		this.sinkChannelName = sinkChannelName;
		this.properties = properties;
		this.definition = properties.get("definition");
		Assert.hasText(this.definition, "Stream properties require a 'definition' value");
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
	 * Return the source module for this stream.
	 * 
	 * @return source module
	 */
	public ModuleDescriptor getSource() {
		return source;
	}

	/**
	 * Return the source channel for this stream (only present if no source module).
	 * 
	 * @return source channel name
	 */
	public String getSourceChannelName() {
		return sourceChannelName;
	}

	/**
	 * Return the ordered list of processors for this stream.
	 * 
	 * @return list of processors
	 */
	public List<ModuleDescriptor> getProcessors() {
		return Collections.unmodifiableList(processors);
	}

	/**
	 * Return the sink for this stream.
	 * 
	 * @return sink module
	 */
	public ModuleDescriptor getSink() {
		return sink;
	}

	/**
	 * Return the sink channel for this stream (only present if no sink module).
	 * 
	 * @return sink channel name
	 */
	public String getSinkChannelName() {
		return sinkChannelName;
	}

	/**
	 * Return true if this stream should be deployed.
	 * 
	 * @return true if this stream should be deployed
	 */
	public boolean isDeploy() {
		return Boolean.parseBoolean(properties.get("deploy"));
	}

	/**
	 * Return an iterator that indicates the order of module deployments for this stream. The modules are returned in
	 * reverse order; i.e. the sink is returned first followed by the processors in reverse order followed by the
	 * source.
	 * 
	 * @return iterator that iterates over the modules in deployment order
	 */
	public Iterator<ModuleDescriptor> getDeploymentOrderIterator() {
		return new DeploymentOrderIterator();
	}

	/**
	 * Return the properties for this stream.
	 * 
	 * @return stream properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the module descriptor for the given module label and type.
	 * 
	 * @param moduleLabel module label
	 * @param moduleType module type
	 * 
	 * @return module descriptor
	 * 
	 * @throws IllegalArgumentException if the module name/type cannot be found
	 */
	public ModuleDescriptor getModuleDescriptor(String moduleLabel, String moduleType) {
		Module.Type type = Module.Type.valueOf(moduleType.toUpperCase());
		ModuleDescriptor moduleDescriptor = null;
		switch (type) {
			case SOURCE:
				moduleDescriptor = getSource();
				break;
			case SINK:
				moduleDescriptor = getSink();
				break;
			case PROCESSOR:
				for (ModuleDescriptor processor : processors) {
					if (processor.getLabel().equals(moduleLabel)) {
						moduleDescriptor = processor;
						break;
					}
				}
		}

		if (moduleDescriptor == null || !moduleLabel.equals(moduleDescriptor.getLabel())) {
			throw new IllegalArgumentException(String.format("Module %s of type %s not found", moduleLabel, moduleType));
		}
		return moduleDescriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		return "Stream{" +
				"name='" + name + '\'' +
				", definition='" + definition +
				"'}";
	}

	/**
	 * Enumeration indicating the state of {@link DeploymentOrderIterator}.
	 */
	enum IteratorState {
		/**
		 * Indicates the iterator has not been used yet.
		 */
		INITIAL,

		/**
		 * Indicates that the first item in the iterator has been returned (the sink module), thus causing the
		 * activation of the processor module iterator.
		 */
		PROC_ITERATOR_ACTIVE,

		/**
		 * Indicates that the last item (the source module) has been iterated over. Any subsequent iteration will result
		 * in {@link java.util.NoSuchElementException}
		 */
		LAST_ITERATED
	}

	/**
	 * Iterator that returns the modules in the order in which they should be deployed. The sink module is returned
	 * first, followed by processor modules in reverse order, followed by the source module.
	 */
	class DeploymentOrderIterator implements Iterator<ModuleDescriptor> {

		/**
		 * Iterator state.
		 */
		private IteratorState state;

		/**
		 * Iterator over the processor modules.
		 */
		private final Iterator<ModuleDescriptor> processorIterator;

		/**
		 * Construct a DeploymentOrderIterator.
		 */
		@SuppressWarnings("unchecked")
		DeploymentOrderIterator() {
			Assert.isInstanceOf(Deque.class, processors);
			processorIterator = ((Deque) processors).descendingIterator();
			state = IteratorState.INITIAL;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return state != IteratorState.LAST_ITERATED;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ModuleDescriptor next() {
			switch (state) {
				case INITIAL:
					state = IteratorState.PROC_ITERATOR_ACTIVE;
					if (sink != null) {
						if (!processorIterator.hasNext() && source == null) {
							// no processors and no source (named channel instead)
							state = IteratorState.LAST_ITERATED;
						}
						return sink;
					}
					// no sink (named channel instead), skip ahead
					return next();
				case PROC_ITERATOR_ACTIVE:
					if (processorIterator.hasNext()) {
						ModuleDescriptor descriptor = processorIterator.next();
						if (!processorIterator.hasNext() && source == null) {
							// no processors and no source (named channel instead)
							state = IteratorState.LAST_ITERATED;
						}
						return descriptor;
					}
					else {
						state = IteratorState.LAST_ITERATED;
						return source;
					}
				case LAST_ITERATED:
					throw new NoSuchElementException();
				default:
					throw new IllegalStateException();
			}
		}

		/**
		 * {@inheritDoc}
		 * <p/>
		 * This implementation throws {@link java.lang.UnsupportedOperationException}.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Builder object for {@link Stream} that supports fluent style configuration.
	 */
	public static class Builder {

		/**
		 * Stream name.
		 */
		private String name;

		/**
		 * Source channel name.
		 */
		private String sourceChannelName;

		/**
		 * Sink channel name.
		 */
		private String sinkChannelName;

		/**
		 * Map of module labels to module definitions.
		 */
		private Map<String, ModuleDefinition> moduleDefinitions = new LinkedHashMap<String, ModuleDefinition>();

		/**
		 * Map of module labels to module parameters.
		 */
		private Map<String, Map<String, String>> moduleParameters = new LinkedHashMap<String, Map<String, String>>();

		/**
		 * Stream properties
		 */
		private Map<String, String> properties = Collections.emptyMap();

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
		 * Set the source channel name.
		 * 
		 * @param name source channel name
		 * 
		 * @return this builder
		 */
		public Builder setSourceChannelName(String sourceChannelName) {

			Assert.isTrue(moduleDefinitions.isEmpty()
					|| ModuleType.source != moduleDefinitions.values().iterator().next().getType(),
					"cannot have both a source module and a source channel");
			this.sourceChannelName = sourceChannelName;
			return this;
		}

		/**
		 * Set the sink channel name.
		 * 
		 * @param name sink channel name
		 * 
		 * @return this builder
		 */
		public Builder setSinkChannelName(String sinkChannelName) {
			ArrayList moduleDefinitionList = new ArrayList<ModuleDefinition>(moduleDefinitions.values());
			Assert.isTrue(
					moduleDefinitionList.isEmpty()
							|| ModuleType.sink !=
							moduleDefinitionList.get(moduleDefinitionList.size() - 1),
					"cannot have both a sink module and a sink channel");
			this.sinkChannelName = sinkChannelName;
			return this;
		}

		/**
		 * Add a module definition to this stream builder. Processor modules will be added to the stream in the order
		 * they are added to this builder.
		 * 
		 * @param label label for this module
		 * @param moduleDefinition module definition to add
		 * @return this builder
		 */
		public Builder addModuleDefinition(String label, ModuleDefinition moduleDefinition,
				Map<String, String> parameters) {
			if (moduleDefinitions.containsKey(label)) {
				throw new IllegalArgumentException(String.format("Label %s already in use", label));
			}
			if (ModuleType.source == moduleDefinition.getType()) {
				Assert.isNull(sourceChannelName, "cannot have both a source module and a source channel");
			}
			if (ModuleType.sink == moduleDefinition.getType()) {
				Assert.isNull(sinkChannelName, "cannot have both a sink module and a sink channel");
			}
			moduleDefinitions.put(label, moduleDefinition);
			moduleParameters.put(label, parameters);
			return this;
		}

		/**
		 * Set the properties for the stream.
		 * 
		 * @param properties stream properties
		 * 
		 * @return this builder
		 */
		public Builder setProperties(Map<String, String> properties) {
			this.properties = properties;
			return this;
		}

		/**
		 * Create a new instance of {@link Stream}.
		 * 
		 * @return new Stream instance
		 */
		public Stream build() {
			ModuleDescriptor sourceDescriptor = null;
			ModuleDescriptor sinkDescriptor = null;
			List<ModuleDescriptor> processorDescriptors = new ArrayList<ModuleDescriptor>();

			int i = 0;
			for (Map.Entry<String, ModuleDefinition> entry : moduleDefinitions.entrySet()) {
				String label = entry.getKey();
				ModuleDefinition moduleDefinition = entry.getValue();
				String group = properties.get(String.format("module.%s.group", moduleDefinition.getName()));
				int count = convert(properties.get(String.format("module.%s.count", moduleDefinition.getName())));

				ModuleDescriptor descriptor = new ModuleDescriptor(moduleDefinition, name, label, i, group, count);
				if (!CollectionUtils.isEmpty(moduleDefinition.getComposedModuleDefinitions())) {
					descriptor.setComposed(true);
				}
				// todo: cleanup (and if we do use i here, consider having it in the for loop itself)
				if (i == moduleDefinitions.size() - 1 && sinkChannelName != null) {
					descriptor.setSinkChannelName(sinkChannelName);
				}
				else if (i == 0 && sourceChannelName != null) {
					descriptor.setSourceChannelName(sourceChannelName);
				}
				i++;
				descriptor.addParameters(moduleParameters.get(label));
				switch (moduleDefinition.getType()) {
					case source:
						sourceDescriptor = descriptor;
						break;
					case processor:
						processorDescriptors.add(descriptor);
						break;
					case sink:
						sinkDescriptor = descriptor;
				}
			}

			Assert.isTrue(sourceDescriptor != null || sourceChannelName != null);
			Assert.isTrue(sinkDescriptor != null || sinkChannelName != null);

			return new Stream(name, sourceDescriptor, sourceChannelName, processorDescriptors, sinkDescriptor,
					sinkChannelName, properties);
		}

		/**
		 * Convert a String value to an int. Returns 1 if the String is null.
		 * 
		 * @param s string to convert
		 * 
		 * @return int value of String, or 1 if null
		 */
		private int convert(String s) {
			return s == null ? 1 : Integer.valueOf(s);
		}
	}

}
