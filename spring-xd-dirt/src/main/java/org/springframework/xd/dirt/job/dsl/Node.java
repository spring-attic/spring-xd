/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.job.dsl;

import java.util.Map;

/**
 * Represents a node in a {@link Graph} object that Flo will display as a block.
 *
 * @author Andy Clement
 */
public class Node {

	public final String id;

	public final String name;

	public final Map<String, String> properties;

	Node(String id, String name) {
		this.id = id;
		this.name = name;
		this.properties = null;
	}

	Node(String id, String name, Map<String, String> properties) {
		this.id = id;
		this.name = name;
		this.properties = properties;
	}
}
