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


import static org.springframework.xd.dirt.stream.dsl.XDDSLMessages.NAMED_CHANNELS_UNSUPPORTED_HERE;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;
import org.springframework.xd.dirt.stream.ParsingContext.Position;
import org.springframework.xd.dirt.stream.dsl.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.SinkChannelNode;
import org.springframework.xd.dirt.stream.dsl.SourceChannelNode;
import org.springframework.xd.dirt.stream.dsl.StreamConfigParser;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.StreamNode;
import org.springframework.xd.module.CompositeModuleDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.store.AbstractRepository;

import com.google.common.collect.Iterables;

/**
 * Parser to convert a DSL string for a stream into a list of
 * {@link org.springframework.xd.module.ModuleDescriptor}
 * objects that comprise the given stream.
 *
 * @author Andy Clement
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Eric Bottard
 * @since 1.0
 */
public class XDStreamParser implements XDParser {

	/**
	 * Repository for user defined modules.
	 */
	private final ModuleRegistry moduleRegistry;

	/**
	 * Resolver for module options metadata.
	 */
	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	/**
	 * Optional definition repository used to obtain sub-stream/label
	 * references.
	 *
	 * @see org.springframework.xd.dirt.stream.dsl.StreamConfigParser
	 */
	private CrudRepository<? extends BaseDefinition, String> repository;


	/**
	 * Construct an {@code XDStreamParser}.
	 *
	 * @param repository                     repository for stream definitions (optional)
	 * @param moduleRegistry                 registry for modules
	 * @param moduleOptionsMetadataResolver  resolver for module options metadata
	 */
	public XDStreamParser(CrudRepository<? extends BaseDefinition, String> repository,
			ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		Assert.notNull(moduleRegistry, "moduleRegistry can not be null");
		Assert.notNull(moduleOptionsMetadataResolver, "moduleOptionsMetadataResolver can not be null");
		this.repository = repository;
		this.moduleRegistry = moduleRegistry;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	/**
	 * Construct an {@code XDStreamParser}.
	 *
	 * @param moduleRegistry                 registry for modules
	 * @param moduleOptionsMetadataResolver  resolver for module options metadata
	 */
	public XDStreamParser(ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this(null, moduleRegistry, moduleOptionsMetadataResolver);
	}

	@Override
	public List<ModuleDescriptor> parse(String name, String config, ParsingContext parsingContext) {

		StreamConfigParser parser = new StreamConfigParser(repository);
		StreamNode stream = parser.parse(name, config);
		return buildModuleDescriptors(name, config, parsingContext, stream, null);
	}

	/**
	 * Build a list of ModuleDescriptors out of a parsed StreamNode. If an errorAccumulator
	 * is passed then the method will not exit on the first exception that occurs, instead
	 * it will record the problems in the accumulator and attempt to continue processing.
	 * @param name the name of the definition unit
	 * @param rawDSL the raw DSL text of the definition
	 * @param parsingContext the context in which parsing happens
	 * @param stream the AST construct representing the definition
	 * @param errorAccumulator accumulates exceptions that occur during validation
	 */
	private List<ModuleDescriptor> buildModuleDescriptors(String name, String rawDSL, ParsingContext parsingContext,
			StreamNode stream, List<Exception> errorAccumulator) {
		Deque<ModuleDescriptor.Builder> builders = new LinkedList<>();

		List<ModuleNode> moduleNodes = stream.getModuleNodes();
		for (int m = moduleNodes.size() - 1; m >= 0; m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			ModuleDescriptor.Builder builder =
					new ModuleDescriptor.Builder()
							.setGroup(name)
							.setModuleName(moduleNode.getName())
							.setModuleLabel(moduleNode.getLabelName())
							.setIndex(m);
			if (moduleNode.hasArguments()) {
				ArgumentNode[] arguments = moduleNode.getArguments();
				for (ArgumentNode argument : arguments) {
					builder.setParameter(argument.getName(), argument.getValue());
				}
			}
			builders.add(builder);
		}

		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		if (sourceChannel != null) {
			if (parsingContext.supportsNamedChannels()) {
				builders.getLast().setSourceChannelName(sourceChannel.getChannelName());
			}
			else {
				throw new StreamDefinitionException(rawDSL, sourceChannel.getStartPos(),
						NAMED_CHANNELS_UNSUPPORTED_HERE);
			}
		}

		SinkChannelNode sinkChannel = stream.getSinkChannelNode();
		if (sinkChannel != null) {
			if (parsingContext.supportsNamedChannels()) {
				builders.getFirst().setSinkChannelName(sinkChannel.getChannelName());
			}
			else {
				throw new StreamDefinitionException(rawDSL, sinkChannel.getChannelNode().getStartPos(),
						NAMED_CHANNELS_UNSUPPORTED_HERE);
			}
		}

		// Now that we know about source and sink channel names,
		// do a second pass to determine type. Also convert to composites.
		// And while we're at it (and type is known), validate module name and options
		List<ModuleDescriptor> result = new ArrayList<ModuleDescriptor>(builders.size());
		for (ModuleDescriptor.Builder builder : builders) {
			ModuleType moduleType;
			try {
				moduleType = determineType(builder, builders.size() - 1, parsingContext);
			}
			catch (NoSuchModuleException nsme) {
				if (errorAccumulator != null) {
					errorAccumulator.add(nsme);
					// 'processor' below effectively indicates 'unknown' - a caller passing an
					// error accumulator should be aware that this can happen (the accumulator
					// will contain the exception that indicates it did)
					moduleType = ModuleType.processor;
				}
				else {
					throw nsme;
				}
			}
			builder.setType(moduleType);

			ModuleDefinition moduleDefinition = moduleRegistry.findDefinition(builder.getModuleName(),
					builder.getType());
			if (moduleDefinition != null) {
				builder.setModuleDefinition(moduleDefinition);
				ModuleOptionsMetadata optionsMetadata = moduleOptionsMetadataResolver.resolve(moduleDefinition);
				if (parsingContext.shouldBindAndValidate()) {
					try {
						optionsMetadata.interpolate(builder.getParameters());
					}
					catch (BindException e) {
						ModuleConfigurationException mce = ModuleConfigurationException.fromBindException(
								builder.getModuleName(),
								builder.getType(), e);
						if (errorAccumulator != null) {
							errorAccumulator.add(mce);
						}
						else {
							throw mce;
						}
					}
				}
			}

			result.add(buildModuleDescriptor(builder));
		}
		return result;
	}

	/**
	 * For a given module builder, determine the type of module based on:
	 * <ol>
	 *     <li>module name</li>
	 *     <li>module position in the stream</li>
	 *     <li>presence of (or lack thereof) named channels</li>
	 * </ol>
	 *
	 * @param builder          builder object
	 * @param lastIndex        index of last module in the stream
	 * @param parsingContext   parsing context
	 * @return module type
	 * @throws NoSuchModuleException if the module type does not exist
	 */
	private ModuleType determineType(ModuleDescriptor.Builder builder, int lastIndex, ParsingContext parsingContext) {
		ModuleType moduleType = determineTypeFromNamedChannels(builder, lastIndex, parsingContext);
		if (moduleType != null) {
			return moduleType;
		}
		String name = builder.getModuleName();
		int index = builder.getIndex();

		return resolveModuleType(name, parsingContext.allowed(Position.of(index, lastIndex)));
	}

	/**
	 * Attempt to guess the type of a module given the presence of named channels
	 * references at the start or end of the stream definition.
	 *
	 * @return module type, or null if no named channels were present
	 */
	private ModuleType determineTypeFromNamedChannels(ModuleDescriptor.Builder builder, int lastIndex,
			ParsingContext parsingContext) {
		// Should this fail for composed module too?
		if (parsingContext == ParsingContext.job
				&& (builder.getSourceChannelName() != null || builder.getSinkChannelName() != null)) {
			throw new RuntimeException("TODO");
		}
		ModuleType type = null;
		String moduleName = builder.getModuleName();
		int index = builder.getIndex();
		if (builder.getSourceChannelName() != null) { // preceded by >, so not a source
			if (index == lastIndex) { // this is the final module of the stream
				if (builder.getSinkChannelName() != null) { // but followed by >, so not a sink
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
		else if (builder.getSinkChannelName() != null) { // followed by >, so not a sink
			if (index == 0) { // first module in a stream, and not preceded by >, so IS a source
				type = ModuleType.source;
			}
			else { // not first module, and followed by >, so not a source or sink
				type = ModuleType.processor;
			}
		}
		return (type == null) ? null : resolveModuleType(moduleName, type);
	}

	/**
	 * Return a {@link org.springframework.xd.module.ModuleDescriptor}
	 * per the specifications indicated by the provided builder. If the module
	 * is a composite module, the children modules are also built and included
	 * under {@link org.springframework.xd.module.ModuleDescriptor#getChildren()}.
	 *
	 * @param builder builder object
	 * @return new instance of {@code ModuleDescriptor}
	 */
	private ModuleDescriptor buildModuleDescriptor(ModuleDescriptor.Builder builder) {
		ModuleDefinition def = moduleRegistry.findDefinition(builder.getModuleName(), builder.getType());
		// null if working in 'recovery' mode where something has gone wrong already, it has
		// been logged but continued processing is attempted to uncover any other issues.
		if (def != null && def.isComposed()) {
			String dsl = ((CompositeModuleDefinition) def).getDslDefinition();
			List<ModuleDescriptor> children = parse(def.getName(), dsl, ParsingContext.module);

			// Preserve the options set for the "parent" module in the parameters map
			Map<String, String> parameters = new HashMap<String, String>(builder.getParameters());

			// Pretend that options were set on the composed module itself
			// (eases resolution wrt defaults later)
			for (ModuleDescriptor child : children) {
				for (String key : child.getParameters().keySet()) {
					String prefix = child.getModuleName() + CompositeModule.OPTION_SEPARATOR;
					builder.setParameter(prefix + key, child.getParameters().get(key));
				}
			}

			// This is to copy options from parent to this (which may override
			// what was set above)
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				builder.setParameter(entry.getKey(), entry.getValue());
			}

			// Since ModuleDescriptor is immutable, the children created
			// by the parse method above have to be recreated since the group
			// name needs to be modified
			List<ModuleDescriptor> list = new ArrayList<ModuleDescriptor>();
			for (ModuleDescriptor child : children) {
				ModuleDescriptor.Builder childBuilder =
						ModuleDescriptor.Builder.fromModuleDescriptor(child);
				childBuilder.setGroup(builder.getGroup() + "." + child.getModuleName());
				list.add(childBuilder.build());
			}

			builder.addChildren(list);
		}
		return builder.build();
	}

	/**
	 * Return the module type for the module name. Based on the position
	 * in the stream definition, the module <b>must</b> be one of the
	 * types passed into {@code candidates}.
	 *
	 * @param moduleName name of module
	 * @param candidates the list of module types the module can be
	 * @return the module type for the module name
	 * @throws NoSuchModuleException if no module with this name exists for one of
	 *                               the types present in {@code candidates}
	 */
	private ModuleType resolveModuleType(String moduleName, ModuleType... candidates) {
		for (ModuleType type : candidates) {
			ModuleDefinition def = moduleRegistry.findDefinition(moduleName, type);
			if (def != null) {
				return type;
			}
		}
		throw new NoSuchModuleException(moduleName, candidates);
	}

	/**
	 * A wrapper around the canonical parser that supports multiline "document" parsing.
	 *
	 * <p>While XDStreamParser parses a single definition, this class is able to parse multiple, reporting
	 * errors or accepting constructs like taps against definitions that appear earlier in the document (without
	 * actually saving them in the main repository).</p>
	 *
	 * @author Eric Bottard
	 */
	public static class MultiLineDocumentParser {

		private final XDStreamParser delegate;

		public MultiLineDocumentParser(XDStreamParser delegate) {
			this.delegate = delegate;
		}

		public DocumentParseResult parse(String[] document) {
			DocumentParseResult result = new DocumentParseResult(document.length);
			CrudRepository<BaseDefinition, String> transientRepository = new TransientDefinitionRepository();
			StreamConfigParser parser = new StreamConfigParser(transientRepository);
			int line = 1;
			for (String nameAndDefinitionPair : document) {
				try {
					StreamNode stream = parser.parse(nameAndDefinitionPair);
					String streamName = stream.getStreamName();
					if (streamName == null) {
						// Give the stream a placeholder name as none was supplied. Someone
						// processing the parse result should know this can happen.
						streamName = "UNKNOWN_" + line;
					}
					List<Exception> errorAccumulator = new ArrayList<Exception>();
					List<ModuleDescriptor> moduleDescriptors = delegate.buildModuleDescriptors(streamName,
							nameAndDefinitionPair, ParsingContext.partial_stream, stream, errorAccumulator);
					BaseDefinition streamDefinition = new StreamDefinition(streamName, nameAndDefinitionPair);
					transientRepository.save(streamDefinition);
					result.success(moduleDescriptors, errorAccumulator);
				}
				catch (Exception e) {
					result.failure(e);
				}
				line++;
			}
			return result;
		}


		/**
		 * A throwaway repository that is used during multi-definition parsing. "Sees" definitions that are
		 * known to the real {@link #repository} and stores new definitions in an in-memory map.
		 *
		 * @author Eric Bottard
		 */
		private class TransientDefinitionRepository
				extends AbstractRepository<BaseDefinition, String>
				implements CrudRepository<BaseDefinition, String> {

			private Map<String, BaseDefinition> store = new HashMap<>();

			@Override
			public <S extends BaseDefinition> S save(S entity) {
				store.put(entity.getName(), entity);
				return entity;
			}

			@Override
			public BaseDefinition findOne(String s) {
				BaseDefinition inMemory = store.get(s);
				return inMemory != null ? inMemory : delegate.repository.findOne(s);
			}

			@Override
			public Iterable<BaseDefinition> findAll() {
				return Iterables.concat(delegate.repository.findAll(), store.values());
			}

			@Override
			public long count() {
				return store.size() + delegate.repository.count();
			}

			@Override
			public void delete(String s) {
				store.remove(s);
				delegate.repository.delete(s);
			}

			@Override
			public void delete(BaseDefinition entity) {
				store.remove(entity.getName());
				delegate.repository.delete(entity.getName());
			}

			@Override
			public void deleteAll() {
				store.clear();
				delegate.repository.deleteAll();
			}
		}
	}

}
