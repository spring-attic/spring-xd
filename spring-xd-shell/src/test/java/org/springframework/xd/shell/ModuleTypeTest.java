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

package org.springframework.xd.shell;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.RESTModuleType;


/**
 * Shell integration tests layer is one place where both {@link ModuleType} and {@link RESTModuleType} are visible. This
 * test makes sure they're both kept in sync.
 * 
 * @author Eric Bottard
 */
public class ModuleTypeTest {

	@Test
	public void testSameValues() {
		Set<String> core = new HashSet<String>();
		for (ModuleType type : ModuleType.values()) {
			core.add(type.name());
		}

		Set<String> rest = new HashSet<String>();
		for (RESTModuleType type : RESTModuleType.values()) {
			rest.add(type.name());
		}

		assertEquals(core, rest);
	}

}
