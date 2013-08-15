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
 * A lookup environment is used to resolve a stream. Stream resolution chases
 * down references to substreams or referenced labels/modules.
 *
 * Note:
 * For the initial implementation we use the stream parser class itself but
 * that means when xd-admin is restarted it will not be able to resolve
 * streams. The stream parser also does not know when streams are deleted.
 * Eventually the proper implementation will be based on the stream directory.
 *
 * @author Andy Clement
 */
public interface StreamLookupEnvironment {

	/**
	 * Look up a previously defined stream by name, returns the parsed
	 * AST for it.
	 *
	 * @param name name of the stream
	 * @return AST for the stream
	 */
	StreamNode lookupStream(String name);

	/**
	 * Look up an existing label or module name. The stream name is optionally
	 * supplied to restrict the search to a particular stream.  The returned
	 * value is the computed output channel for that label/module if it is
	 * found. For example:
	 * <pre><code>
	 * mystream = http | foo | file
	 * tap mystream.foo > count
	 * </code></pre>
	 * will cause a lookup for stream 'mystream' module 'foo' when the
	 * tap is being resolved.  The return value would be 'mystream.1' (The
	 * output channel for a module is based on the index of the module within
	 * the stream, starting at 0).
	 *
	 * @param streamName the name of the stream
	 * @param labelOrModuleName a label or name of a module
	 * @return the label or module name
	 */
	String lookupChannelForLabelOrModule(String streamName, String labelOrModuleName);

}