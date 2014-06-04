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

package org.springframework.xd.dirt.stream.dsl;


/**
 * A lookup environment is used to resolve a stream. Stream resolution chases down references to substreams or
 * referenced labels/modules.
 * 
 * Note: For the initial implementation we use the stream parser class itself but that means when xd-admin is restarted
 * it will not be able to resolve streams. The stream parser also does not know when streams are deleted. Eventually the
 * proper implementation will be based on the stream directory.
 * 
 * @author Andy Clement
 */
public interface StreamLookupEnvironment {

	/**
	 * Look up a previously defined stream by name, returns the parsed AST for it.
	 * 
	 * @param name name of the stream
	 * @return AST for the stream
	 */
	StreamNode lookupStream(String name);

}
