/*
 * Copyright 2014 the original author or authors.
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

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import org.springframework.xd.module.options.mixins.BatchJobCommitIntervalOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobDeleteFilesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldNamesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes the options for the filepollhdfs job.
 * 
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Thomas Risberg
 */
@Mixin({ BatchJobRestartableOptionMixin.class, BatchJobDeleteFilesOptionMixin.class,
	BatchJobFieldNamesOptionMixin.class, BatchJobCommitIntervalOptionMixin.class, HadoopConfigurationMixin.class })
public class FilePollHdfsJobOptionsMetadata {

	private String fileName = XD_JOB_NAME;

	private int rollover = 1000000;

	private String directory = "/xd/" + XD_JOB_NAME;

	private String fileExtension = "csv";

	@ModuleOption("the filename to use in HDFS")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@ModuleOption("the number of bytes to write before creating a new file in HDFS")
	public void setRollover(int rollover) {
		this.rollover = rollover;
	}

	@ModuleOption("the directory to write the file(s) to in HDFS")
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@ModuleOption("the file extension to use")
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFileName() {
		return fileName;
	}

	public int getRollover() {
		return rollover;
	}

	public String getDirectory() {
		return directory;
	}

	public String getFileExtension() {
		return fileExtension;
	}
}
