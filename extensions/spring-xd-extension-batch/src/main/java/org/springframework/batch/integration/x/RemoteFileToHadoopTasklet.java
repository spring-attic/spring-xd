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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.hadoop.store.output.OutputStreamWriter;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;


/**
 * Retrieves a fileName from the step execution context; uses the {@link RemoteFileTemplate} to retrieve the file as a
 * String and writes to hdfs.
 * 
 * @author Gary Russell
 */
public class RemoteFileToHadoopTasklet implements Tasklet {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RemoteFileTemplate<?> template;

	private final Configuration configuration;

	private final String hdfsDirectory;

	@SuppressWarnings("rawtypes")
	public RemoteFileToHadoopTasklet(RemoteFileTemplate template, Configuration configuration, String hdfsDirectory) {
		Assert.notNull(template, "'template' cannot be null");
		Assert.notNull(configuration, "'configuration' cannot be null");
		Assert.notNull(hdfsDirectory, "'hdfsDirectory' cannot be null");
		this.template = template;
		this.configuration = configuration;
		this.hdfsDirectory = hdfsDirectory.endsWith("/") ? hdfsDirectory : (hdfsDirectory + "/");
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		final String filePath = chunkContext.getStepContext().getStepExecution().getExecutionContext().getString(
				"filePath");
		Assert.notNull(filePath);
		if (logger.isDebugEnabled()) {
			logger.debug("Transferring " + filePath + " to HDFS");
		}
		boolean result = this.template.get(filePath, new InputStreamCallback() {

			@Override
			public void doWithInputStream(InputStream stream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(configuration,
						new Path(hdfsDirectory + filePath), null);
				byte[] buff = new byte[1024];
				int len;
				while ((len = stream.read(buff)) > 0) {
					if (len == buff.length) {
						writer.write(buff);
					}
					else {
						writer.write(Arrays.copyOf(buff, len));
					}
				}
				writer.close();
			}

		});
		if (!result) {
			throw new MessagingException("Error during file transfer");
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Transferred " + filePath + " to HDFS");
			}
			return RepeatStatus.FINISHED;
		}
	}

}
