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

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.validation.DateFormat;

/**
 * Options for the {@code timestampfile} job module.
 *
 * @author Patrick Peralta
 */
@Mixin({ BatchJobRestartableOptionMixin.class })
public class TimestampFileJobOptionsMetadata {

	/**
	 * Name of file to write timestamps to. Excludes parent directory
	 * and file extension.
	 */
	private String fileName = XD_JOB_NAME;

	/**
	 * Name of directory to write timestamp file to.
	 */
	private String directory = "/tmp/xd/output/";

	/**
	 * Extension for file to write timestamps to.
	 */
	private String fileExtension = "txt";

	/**
	 * Time stamp date format.
	 */
	private String format = "yyyy-MM-dd HH:mm:ss";

	@ModuleOption("the filename to write time stamps to")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@ModuleOption("the directory to write the timestamp file to")
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@ModuleOption("the file extension to use")
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	@ModuleOption("how to render the current time, using SimpleDateFormat")
	public void setFormat(String format) {
		this.format = format;
	}

	public String getFileName() {
		return fileName;
	}

	public String getDirectory() {
		return directory;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	@DateFormat
	public String getFormat() {
		return format;
	}

}
