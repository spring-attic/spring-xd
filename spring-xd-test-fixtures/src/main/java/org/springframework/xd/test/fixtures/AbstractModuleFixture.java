/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * Base class for objects that can be incorporated in a stream definition by calling their {@link #toString()} method.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
public abstract class AbstractModuleFixture<F extends AbstractModuleFixture<F>> {

	protected String label;

	@Override
	public final String toString() {
		return toDSL();
	}

	/**
	 * Returns a representation of the module suitable for inclusion in a stream definition, <i>e.g.</i>
	 * {@code file --dir=xxxx --name=yyyy}
	 */
	protected abstract String toDSL();


	/**
	 * Sets the label for the fixture.
	 * @param label The name to be associated with the label.
	 * @return the current instance of the module fixture.
	 */
	@SuppressWarnings("unchecked")
	public F label(String label) {
		Assert.hasText(label, "Label should not be null nor empty");
		this.label = label;
		return (F) this;
	}

}
