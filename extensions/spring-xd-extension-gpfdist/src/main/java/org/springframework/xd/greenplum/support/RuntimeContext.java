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
package org.springframework.xd.greenplum.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime context for load operations.
 *
 * @author Janne Valkealahti
 *
 */
public class RuntimeContext {

	private final List<String> locations = new ArrayList<String>();

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations.clear();
		this.locations.addAll(locations);
	}

	public void addLocation(String location) {
		this.locations.add(location);
	}

}
