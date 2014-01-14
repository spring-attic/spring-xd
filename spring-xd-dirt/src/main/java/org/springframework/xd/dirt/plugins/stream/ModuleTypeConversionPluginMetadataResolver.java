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

package org.springframework.xd.dirt.plugins.stream;

import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.FlattenedCompositeModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PojoModuleOptionsMetadata;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * A {@link ModuleOptionsMetadataResolver} that will dynamically add {@code inputType} and {@code outputType} options to
 * every module, according to their type.
 * 
 * @see ModuleTypeConversionPlugin
 * @author Eric Bottard
 */
public class ModuleTypeConversionPluginMetadataResolver implements ModuleOptionsMetadataResolver {

	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
		List<ModuleOptionsMetadata> moms = new ArrayList<ModuleOptionsMetadata>();
		ModuleType type = moduleDefinition.getType();
		if (type == source || type == processor) {
			moms.add(new PojoModuleOptionsMetadata(Output.class));
		}
		if (type == sink || type == processor) {
			moms.add(new PojoModuleOptionsMetadata(Input.class));
		}
		return new FlattenedCompositeModuleOptionsMetadata(moms);
	}

	/**
	 * Provides info about the {@code inputType} option.
	 * 
	 * @author Eric Bottard
	 */
	public static class Input {

		private MediaType inputType;

		public MediaType getInputType() {
			return inputType;
		}

		@ModuleOption("how this module should interpret messages it consumes")
		public void setInputType(MediaType inputType) {
			this.inputType = inputType;
		}
	}

	/**
	 * Provides info about the {@code outputType} option.
	 * 
	 * @author Eric Bottard
	 */
	public static class Output {

		private MediaType outputType;

		public MediaType getOutputType() {
			return outputType;
		}


		@ModuleOption("how this module should emit messages it produces")
		public void setOutputType(MediaType outputType) {
			this.outputType = outputType;
		}

	}

}
