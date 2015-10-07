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

import java.util.HashMap;
import java.util.Map;


/**
 * Represents a node in a {@link Graph} object that Flo will display as a block.
 *
 * @author Andy Clement
 */
public class Link {

	public int from;

	public int to;

	/**
	 * Properties on a link can capture the name of a potential transition that
	 * would lead to this link being taken when the 'from' job completes.
	 */
	public Map<String, String> properties = null;

	public Link(int sourceId, int targetId) {
		this.from = sourceId;
		this.to = targetId;
	}

	public Link(int sourceId, int targetId, String transitionName) {
		this.from = sourceId;
		this.to = targetId;
		properties = new HashMap<>();
		properties.put("transitionName", transitionName);
	}
}
