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

package org.springframework.xd.dirt.modules.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

/**
 * Integration test class to do various tests about {@link ModuleOptionsMetadata} provided by XD.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsMetadataSanityTests {

	private ModuleRegistry moduleRegistry = new ResourceModuleRegistry("file:../modules");

	private Map<String, Integer> counts = new TreeMap<String, Integer>() {

		@Override
		public Integer get(Object key) {
			Integer i = super.get(key);
			if (i == null) {
				i = 0;
				put((String) key, 0);
			}
			return i;
		}
	};

	@Test
	public void sanityChecks() {
		for (ModuleType moduleType : ModuleType.values()) {
			for (ModuleDefinition def : moduleRegistry.findDefinitions(moduleType)) {
				ModuleOptionsMetadata moduleOptionsMetadata = def.getModuleOptionsMetadata();
				for (ModuleOption mo : moduleOptionsMetadata) {
					assertNotNull(
							String.format("ModuleOption type should be provided for %s:%s/%s", moduleType,
									def.getName(), mo.getName()), mo.getType());
					assertFalse(
							String.format("ModuleOption description for %s:%s/%s should start with lowercase : '%s'",
									moduleType, def.getName(), mo.getName(), mo.getDescription()),
							Character.isUpperCase(mo.getDescription().charAt(0)));
					assertFalse(String.format("ModuleOption description for %s:%s/%s should not end with a dot : '%s'",
							moduleType, def.getName(), mo.getName(), mo.getDescription()),
							mo.getDescription().endsWith("."));

					counts.put(mo.getName(), counts.get(mo.getName()) + 1);
				}
			}
		}


		// for (String o : counts.keySet()) {
		// System.out.format("%s : %d%n", o, counts.get(o));
		// }
	}

}
