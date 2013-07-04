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
 * Central service for dealing with streams in the system.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 */
public interface StreamDeployer {

	/**
	 * Deploy a new stream in the system.
	 * 
	 * @param name a key under which the stream will be known from now on
	 * @param config a DSL representation of what the stream does
	 * @return a runtime representation of the stream definition
	 * @throws StreamAlreadyExistsException if a stream already exists with the given name
	 */
	StreamDefinition deployStream(String name, String config);

	/**
	 * Undeploy an already existing stream from the system.
	 * 
	 * @param name the key under which the stream had previously been deployed
	 * @return a runtime representation of the (now to be considered gone) stream definition
	 * @throws NoSuchStreamException if no stream exists with that name
	 */
	StreamDefinition undeployStream(String name);
}
