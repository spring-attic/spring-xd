/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.springframework.xd.dirt.stream.ParsingContext.stream;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.XDDSLMessages;
import org.springframework.xd.module.ModuleDefinition;

/**
 * Default implementation of {@link StreamDeployer} that emits deployment request messages on a bus and relies on
 * {@link StreamDefinitionRepository} and {@link StreamRepository} for persistence.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class StreamDeployer extends AbstractInstancePersistingDeployer<StreamDefinition, Stream> {

	private final XDParser parser;

	public StreamDeployer(StreamDefinitionRepository repository,
			StreamRepository streamRepository, XDParser parser) {
		super(repository, streamRepository, parser, stream);
		this.parser = parser;
	}

	@Override
	protected Stream makeInstance(StreamDefinition definition) {
		return new Stream(definition);
	}

	@Override
	protected StreamDefinition createDefinition(String name, String definition, boolean deploy) {
		return new StreamDefinition(name, definition, deploy);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Before deleting the stream, perform an undeploy. This causes a graceful shutdown of the modules in the stream
	 * before it is deleted from the repository.
	 */
	@Override
	protected void beforeDelete(StreamDefinition definition) {
		super.beforeDelete(definition);
		// todo: this parsing and setting of ModuleDefinitions should not be needed once we refactor the
		// ModuleDependencyRepository so that the dependencies are also stored in ZooKeeper.
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		try {
			for (ModuleDeploymentRequest request : parser.parse(definition.getName(), definition.getDefinition(),
					ParsingContext.stream)) {
				moduleDefinitions.add(new ModuleDefinition(request.getModule(), request.getType()));
			}
		}
		catch (StreamDefinitionException e) {
			// we can ignore an exception for a tap whose stream no longer exists
			if (!(XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE.equals(e.getMessageCode())
					&& definition.getDefinition().trim().startsWith("tap:"))) {
				throw e;
			}
		}
		definition.setModuleDefinitions(moduleDefinitions);
		basicUndeploy(definition.getName());
	}

}
