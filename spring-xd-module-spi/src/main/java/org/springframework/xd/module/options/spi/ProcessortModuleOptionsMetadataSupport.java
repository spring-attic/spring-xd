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

package org.springframework.xd.module.options.spi;

import org.springframework.http.MediaType;


/**
 * Provides info about the {@code inputType} and {@code outputType} options.
 * 
 * @author Eric Bottard
 */
public abstract class ProcessortModuleOptionsMetadataSupport {

	private MediaType inputType;

	private MediaType outputType;


	public MediaType getInputType() {
		return inputType;
	}

	@ModuleOption("how this module should interpret messages it consumes")
	public void setInputType(MediaType inputType) {
		this.inputType = inputType;
	}


	public MediaType getOutputType() {
		return outputType;
	}


	@ModuleOption("how this module should emit messages it produces")
	public void setOutputType(MediaType outputType) {
		this.outputType = outputType;
	}


}
