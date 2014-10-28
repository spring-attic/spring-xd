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

package org.springframework.batch.integration.x;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.batch.core.step.tasklet.Tasklet} implementation
 * that writes the current timestamp to a file.
 *
 * @author Patrick Peralta
 */
public class TimestampFileTasklet implements Tasklet, InitializingBean {

	/**
	 * File separator.
	 */
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Name of file to write timestamps to. Excludes parent directory
	 * and file extension.
	 */
	private String fileName;

	/**
	 * Name of directory to write timestamp file to.
	 */
	private String directory;

	/**
	 * Extension for file to write timestamps to.
	 */
	private String fileExtension;

	/**
	 * Time stamp date format.
	 *
	 * @see java.text.SimpleDateFormat
	 */
	private String format;


	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(fileName, "fileName required");
		Assert.hasText(directory, "directory required");
		Assert.hasText(fileExtension, "fileExtension required");
		Assert.hasText(format, "format required");
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		ensureDirectory();

		String name = getDirectory() + FILE_SEPARATOR + getFileName() + '.' + getFileExtension();
		DateFormat dateFormat = new SimpleDateFormat(getFormat());
		PrintWriter writer =  null;

		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(name)));
			writer.println(dateFormat.format(new Date()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}

		return RepeatStatus.FINISHED;
	}

	private void ensureDirectory() throws IOException {
		File dir = new File(getDirectory());
		if (!dir.exists()) {
			FileUtils.forceMkdir(dir);
		}
		Assert.isTrue(dir.exists());
	}

}
