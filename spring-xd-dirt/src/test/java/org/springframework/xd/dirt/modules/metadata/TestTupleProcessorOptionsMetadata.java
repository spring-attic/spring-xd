/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionPluginMetadataResolver;
import org.springframework.xd.module.options.PojoModuleOptionsMetadata;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.tuple.DefaultTuple;


/**
 * 
 * @author David Turanski
 */
public class TestTupleProcessorOptionsMetadata {

	/**
	 * TODO: returning a class here doesn't work
	 * 
	 * @return
	 */
	public String getInputType() {
		return DefaultTuple.class.getName();
	}

	/**
	 * TODO: Currrently an annotated setter is required for all options by {@link PojoModuleOptionsMetadata}. In this
	 * case, the --inputType option is not actually set here. Instead it is processed by the
	 * {@link ModuleTypeConversionPluginMetadataResolver}.
	 * 
	 * @param inputType
	 */
	@ModuleOption("the default input type for this module is Tuple")
	public void setInputType(String inputType) {
	}

}
