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
 * A runtime representation of a deployed {@link TapDefinition}.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public class TapInstance extends BaseInstance<TapDefinition> {

	/**
	 * Create a new Tap instance out of the given {@link StreamDefinition}.
	 */
	public TapInstance(TapDefinition definition) {
		super(definition);
	}
}
