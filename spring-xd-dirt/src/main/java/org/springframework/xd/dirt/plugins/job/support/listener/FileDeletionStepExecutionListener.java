/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job.support.listener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.io.Resource;
import org.springframework.xd.dirt.plugins.job.ExpandedJobParametersConverter;

/**
 * Step listener which can be used to delete files once they have been successfully processed by XD.
 *
 * Can be used in one of two different ways:
 *
 * <ol>
 *     <li>If the 'resources' property is set, it will try to delete each resource as a file.</li>
 *     <li>Otherwise, it will try to locate the 'absoluteFilePath' parameter in the job parameters
 *     	   and delete this file if it exists.</li>
 * </ol>
 *
 * @author Luke Taylor
 * @author Michael Minella
 * @author Gary Russell
 */
public class FileDeletionStepExecutionListener implements StepExecutionListener {

	private boolean deleteFiles;

	private Resource[] resources;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void beforeStep(StepExecution stepExecution) {
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (!deleteFiles) {
			return stepExecution.getExitStatus();
		}

		if (stepExecution.getStatus().equals(BatchStatus.STOPPED) || stepExecution.getStatus().isUnsuccessful()) {
			logger.warn("Job is stopped, or failed to complete successfully. File deletion will be skipped");
			return stepExecution.getExitStatus();
		}

		if (resources != null) {
			deleteResources();
		}
		else {
			deleteFilePath(stepExecution.getJobExecution().getJobParameters().getString(
					ExpandedJobParametersConverter.ABSOLUTE_FILE_PATH));
		}
		return stepExecution.getExitStatus();
	}

	private void deleteResources() {
		for (Resource r : resources) {
			if (r.exists()) {
				try {
					r.getFile().delete();
				}
				catch (IOException e) {
					logger.error("Failed to delete " + r, e);
				}
			}
		}
	}

	private void deleteFilePath(String filePath) {
		if (filePath == null) {
			// Nothing to delete
			return;
		}

		File f = new File(filePath);
		if (f.exists()) {
			logger.info("Deleting file " + filePath);
			f.delete();
		}
		else {
			logger.warn("File '" + filePath + "' does not exist.");
		}

	}

	public void setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
	}

	public void setResources(Resource[] resources) {
		this.resources = Arrays.copyOf(resources, resources.length);
	}
}
