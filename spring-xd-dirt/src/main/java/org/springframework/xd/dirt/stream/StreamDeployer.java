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

	public StreamDeployer(StreamDefinitionRepository repository, DeploymentMessageSender messageSender,
			StreamRepository streamRepository, XDParser parser) {
		super(repository, streamRepository, messageSender, parser, "stream");
	}

	@Override
	protected Stream makeInstance(StreamDefinition definition) {
		return new Stream(definition);
	}

}
