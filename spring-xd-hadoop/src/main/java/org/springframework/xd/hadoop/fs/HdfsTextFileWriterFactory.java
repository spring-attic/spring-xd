/*
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.hadoop.fs;

import org.apache.hadoop.fs.FileSystem;

import org.springframework.util.Assert;

/**
 * 
 * @author Mark Pollack
 */
public class HdfsTextFileWriterFactory implements HdfsWriterFactory {

	private FileSystem fileSystem;

	public static final String DEFAULT_BASE_FILENAME = "data";

	public static final String DEFAULT_BASE_PATH = "/data/";

	public static final String DEFAULT_FILE_SUFFIX = "log";

	public static long DEFAULT_ROLLOVER_THRESHOLD_IN_BYTES = 10 * 1024 * 1024; // 10MB

	private long rolloverThresholdInBytes = DEFAULT_ROLLOVER_THRESHOLD_IN_BYTES;

	private String baseFilename = DEFAULT_BASE_FILENAME;

	private String basePath = DEFAULT_BASE_PATH;

	private String fileSuffix = DEFAULT_FILE_SUFFIX;

	public HdfsTextFileWriterFactory(FileSystem fileSystem) {
		Assert.notNull(fileSystem, "Hadoop FileSystem must not be null.");
		this.fileSystem = fileSystem;
	}

	public String getFileSuffix() {
		return fileSuffix;
	}

	public void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

	public String getBaseFilename() {
		return baseFilename;
	}

	public void setBaseFilename(String baseFilename) {
		this.baseFilename = baseFilename;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public long getRolloverThresholdInBytes() {
		return rolloverThresholdInBytes;
	}

	public void setRolloverThresholdInBytes(long rolloverThresholdInBytes) {
		this.rolloverThresholdInBytes = rolloverThresholdInBytes;
	}


	@Override
	public HdfsWriter createWriter() {
		HdfsTextFileWriter textFileWriter = new HdfsTextFileWriter(fileSystem);
		textFileWriter.setBasePath(basePath);
		textFileWriter.setBaseFilename(baseFilename);
		textFileWriter.setFileSuffix(fileSuffix);
		textFileWriter.setRolloverThresholdInBytes(rolloverThresholdInBytes);
		return textFileWriter;
	}

}
