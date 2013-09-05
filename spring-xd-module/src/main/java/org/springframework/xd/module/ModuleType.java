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

package org.springframework.xd.module;

/**
 * @author Glenn Renfro
 */
public enum ModuleType {
	SOURCE("source"), PROCESSOR("processor"), SINK("sink"), JOB("job");

	String typeName;

	ModuleType(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public String toString() {
		return this.typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public boolean equals(String s) {
		if (valueOf(s.toUpperCase()).equals(this)) {
			return true;
		}
		return false;
	}

	public static ModuleType getModuleTypeByTypeName(String typeName) {
		if (typeName.equals(SOURCE.getTypeName())) {
			return SOURCE;
		}
		if (typeName.equals(PROCESSOR.getTypeName())) {
			return PROCESSOR;
		}
		if (typeName.equals(SINK.getTypeName())) {
			return SINK;
		}
		if (typeName.equals(JOB.getTypeName())) {
			return JOB;
		}
		return null;
	}
}
