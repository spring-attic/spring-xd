package org.springframework.xd.dirt.plugins.job.support.listener;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.core.io.Resource;
import org.springframework.xd.dirt.plugins.job.ExpandedJobParametersConverter;

/**
 * Job listener which can be used to deleted files once they have been successfully processed by XD.
 *
 * Can be used in one of two different ways:
 *
 * 1. If the 'resources' property is set, it will try to delete each resource as a file.
 * 2. Otherwise, it will try to locate the 'absoluteFilePath' parameter in the job parameters and
 *    delete this file if it exists.
 *
 * @author Luke Taylor
 */
public class FileDeletionJobExecutionListener implements JobExecutionListener {
	private boolean deleteFiles;
	private Resource[] resources;
	private Log logger = LogFactory.getLog(getClass());

	public void beforeJob(JobExecution jobExecution) {
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (!deleteFiles) {
			return;
		}

		if (jobExecution.getStatus().equals(BatchStatus.STOPPED) || jobExecution.getStatus().isUnsuccessful()) {
			logger.warn("Job is stopped, or failed to complete successfully. File deletion will be skipped");
			return;
		}

		if (resources != null) {
			deleteResources();
		}
		else {
			deleteFilePath(jobExecution.getJobParameters().getString(ExpandedJobParametersConverter.ABSOLUTE_FILE_PATH));
		}
	}

	private void deleteResources() {
		for(Resource r : resources) {
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
		this.resources = resources;
	}
}

