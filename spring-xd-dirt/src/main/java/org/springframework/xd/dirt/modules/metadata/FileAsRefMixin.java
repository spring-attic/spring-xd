/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 * A mixin that can be used every time a module dealing with Files offers
 * the choice to pass along the File itself or its contents.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class FileAsRefMixin implements ProfileNamesProvider {

	private FileReadingMode fileReadingmode = FileReadingMode.contents;

	private Boolean withMarkers = null;

	@NotNull
	public FileReadingMode getMode() {
		return fileReadingmode;
	}

	@ModuleOption("specifies how the file is being read. By default the content of a file is provided as byte array")
	public void setMode(FileReadingMode mode) {
		this.fileReadingmode = mode;
	}

	public Boolean getWithMarkers() {
		return withMarkers;
	}

	@ModuleOption(defaultValue = "false",
			value = "if true emits start of file/end of file marker messages before/after the data. Only valid with FileReadingMode 'lines'")
	public void setWithMarkers(Boolean withMarkers) {
		this.withMarkers = withMarkers;
	}

	@AssertTrue(message = "withMarkers can only be supplied when FileReadingMode is 'lines'")
	public boolean isWithMarkersValid() {
		if (this.withMarkers != null && !FileReadingMode.lines.equals(this.fileReadingmode)) {
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public String[] profilesToActivate() {
		return new String[] { this.fileReadingmode.getProfile() };
	}

}
