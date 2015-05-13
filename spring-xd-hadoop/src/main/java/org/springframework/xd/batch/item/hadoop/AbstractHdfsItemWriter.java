/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.batch.item.hadoop;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import org.apache.hadoop.fs.Path;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.data.hadoop.fs.FsShell;
import org.springframework.xd.hadoop.fs.HdfsTextFileWriterFactory;

/**
 * 
 * @author Mark Pollack
 */
public abstract class AbstractHdfsItemWriter<T> extends AbstractItemStreamItemWriter<T> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final AtomicLong counter = new AtomicLong(0L);

	private final AtomicLong bytesWritten = new AtomicLong(0L);

	private volatile boolean initialized;

	private String baseFilename = HdfsTextFileWriterFactory.DEFAULT_BASE_FILENAME;

	private String basePath = HdfsTextFileWriterFactory.DEFAULT_BASE_PATH;

	private String fileSuffix = HdfsTextFileWriterFactory.DEFAULT_FILE_SUFFIX;

	private long rolloverThresholdInBytes = HdfsTextFileWriterFactory.DEFAULT_ROLLOVER_THRESHOLD_IN_BYTES;

	public abstract void write(List<? extends T> items) throws Exception;

	public abstract FileSystem getFileSystem();

	protected void initializeCounterIfNecessary() throws IOException {
		if (!initialized) {
			FsShell fsShell = new FsShell(getFileSystem().getConf(), getFileSystem());

			if (getFileSystem().exists(new Path(getBasePath()))) {
				int maxCounter = 0;
				boolean foundFile = false;
				Collection<FileStatus> fileStats = fsShell.ls(this.getBasePath());
				for (FileStatus fileStatus : fileStats) {
					String shortName = fileStatus.getPath().getName();
					int counterFromName = getCounterFromName(shortName);
					if (counterFromName != -1) {
						foundFile = true;
					}
					if (counterFromName > maxCounter) {
						maxCounter = counterFromName;
					}
				}
				if (foundFile) {
					logger.debug("Found " + maxCounter + " existing files");
					this.setCounter(maxCounter + 1);
				}
			}
			else {
				logger.info("Creating base path " + getBasePath());
				fsShell.mkdir(getBasePath());
			}
			initialized = true;
		}
	}

	protected void reset() {
		initialized = false;
	}

	protected int getCounterFromName(String shortName) {
		Pattern pattern = Pattern.compile(baseFilename + "-([\\d+]{1,})");
		Matcher matcher = pattern.matcher(shortName);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return -1;
	}

	public long getRolloverThresholdInBytes() {
		return rolloverThresholdInBytes;
	}

	public void setRolloverThresholdInBytes(long rolloverThresholdInBytes) {
		this.rolloverThresholdInBytes = rolloverThresholdInBytes;
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

	public long getCounter() {
		return counter.get();
	}

	public void setCounter(long value) {
		counter.set(value);
	}

	public void incrementCounter() {
		counter.incrementAndGet();
	}

	public void incrementBytesWritten(long bytesWritten) {
		this.bytesWritten.addAndGet(bytesWritten);
	}

	public void resetBytesWritten() {
		this.bytesWritten.set(0L);
	}

	public long getBytesWritten() {
		return bytesWritten.get();
	}

	public String getFileName() {
		return basePath + baseFilename + "-" + getCounter() + "." + fileSuffix;
	}

}
