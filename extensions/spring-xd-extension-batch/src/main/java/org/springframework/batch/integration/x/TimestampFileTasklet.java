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
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.batch.core.step.tasklet.Tasklet} implementation
 * that writes the current timestamp to a file.
 *
 * @author Patrick Peralta
 */
public class TimestampFileTasklet implements Tasklet {

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
	private final String fileName;

	/**
	 * Name of directory to write timestamp file to.
	 */
	private final String directory;

	/**
	 * Extension for file to write timestamps to.
	 */
	private final String fileExtension;

	/**
	 * Time stamp date format.
	 *
	 * @see java.text.SimpleDateFormat
	 */
	private final String format;


	public TimestampFileTasklet(String fileName, String directory, String fileExtension, String format) {
		Assert.hasText(fileName, "fileName required");
		Assert.hasText(directory, "directory required");
		Assert.hasText(fileExtension, "fileExtension required");
		Assert.hasText(format, "format required");

		this.fileName = fileName;
		this.directory = directory;
		this.fileExtension = fileExtension;
		this.format = format;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
		ensureDirectory();

		String name = directory + FILE_SEPARATOR + fileName + '.' + fileExtension;
		DateFormat dateFormat = new SimpleDateFormat(format);
		PrintWriter writer = null;

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
		File dir = new File(directory);
		if (!dir.exists()) {
			FileUtils.forceMkdir(dir);
		}
		Assert.isTrue(dir.exists());
		Assert.isTrue(dir.canWrite());
	}

}
