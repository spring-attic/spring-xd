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

package org.springframework.xd.dirt.test.process;

import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;


/**
 * A helper class to easily create {@link AbstractSingleNodeProcessingChain}s using static methods.
 * 
 * @author David Turanski
 */
public final class SingleNodeProcessingChainSupport {

	private SingleNodeProcessingChainSupport() {

	}

	/**
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (no source or sink, only a processing chain}
	 * @return a {@link SingleNodeProcessingChain}
	 */
	public static final SingleNodeProcessingChain chain(SingleNodeApplication application, String streamName,
			String processingChain) {
		return new SingleNodeProcessingChain(application, streamName, processingChain);
	}

	/**
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (no source or sink, only a processing chain}
	 * @param moduleResourceLocation a resource location pointing to a local directory from where module definitions may
	 *        be registered
	 * @return a {@link SingleNodeProcessingChain}
	 */
	public static final SingleNodeProcessingChain chain(SingleNodeApplication application, String streamName,
			String processingChain, String moduleResourceLocation) {
		return new SingleNodeProcessingChain(application, streamName, processingChain, moduleResourceLocation);
	}

	/**
	 * 
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (must include a source and no sink}
	 * @return a {@link SingleNodeProcessingChainConsumer}
	 */
	public static final SingleNodeProcessingChainConsumer chainConsumer(SingleNodeApplication application,
			String streamName, String processingChain) {
		return new SingleNodeProcessingChainConsumer(application, streamName, processingChain);
	}

	/**
	 * 
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (must include a source and no sink}
	 * @param moduleResourceLocation a resource location pointing to a local directory from where module definitions may
	 *        be registered
	 * @return a {@link SingleNodeProcessingChainConsumer}
	 */
	public static final SingleNodeProcessingChainConsumer chainConsumer(SingleNodeApplication application,
			String streamName, String processingChain, String moduleResourceLocation) {
		return new SingleNodeProcessingChainConsumer(application, streamName, processingChain, moduleResourceLocation);
	}

	/**
	 * 
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (must include a sink and no source}
	 * @return a {@link SingleNodeProcessingChainProducer}
	 */
	public static final SingleNodeProcessingChainProducer chainProducer(SingleNodeApplication application,
			String streamName, String processingChain) {
		return new SingleNodeProcessingChainProducer(application, streamName, processingChain);
	}

	/**
	 * 
	 * @param application the {@link SingleNodeApplication} to use
	 * @param streamName the name of the stream to create
	 * @param processingChain the partial stream definition DSL (must include a sink and no source}
	 * @param moduleResourceLocation a resource location pointing to a local directory from where module definitions may
	 *        be registered
	 * @return a {@link SingleNodeProcessingChainProducer}
	 */
	public static final SingleNodeProcessingChainProducer chainProducer(SingleNodeApplication application,
			String streamName, String processingChain, String moduleResourceLocation) {
		return new SingleNodeProcessingChainProducer(application, streamName, processingChain, moduleResourceLocation);
	}
}
