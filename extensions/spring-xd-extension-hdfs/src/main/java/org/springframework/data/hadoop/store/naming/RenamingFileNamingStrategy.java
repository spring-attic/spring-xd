/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.data.hadoop.store.naming;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.springframework.context.ApplicationListener;
import org.springframework.data.hadoop.store.event.AbstractStorageEvent;
import org.springframework.data.hadoop.store.event.FileWrittenEvent;
import org.springframework.util.Assert;

/**
 * A {@code FileNamingStrategy} which add tmp postfix and renames it back if event is received.
 * 
 * @author Janne Valkealahti
 * 
 */
public class RenamingFileNamingStrategy extends AbstractFileNamingStrategy implements
		ApplicationListener<AbstractStorageEvent> {

	private static final Log log = LogFactory.getLog(RenamingFileNamingStrategy.class);

	private Configuration configuration;

	private final static String DEFAULT_POSTFIX = ".tmp";

	private String suffix;

	/**
	 * Instantiates a new renaming file naming strategy.
	 */
	public RenamingFileNamingStrategy() {
		this(null);
	}

	/**
	 * Instantiates a new renaming file naming strategy.
	 * 
	 * @param configuration the configuration
	 */
	public RenamingFileNamingStrategy(Configuration configuration) {
		this(configuration, DEFAULT_POSTFIX);
	}

	/**
	 * Instantiates a new renaming file naming strategy.
	 * 
	 * @param configuration the configuration
	 * @param suffix the suffix
	 */
	public RenamingFileNamingStrategy(Configuration configuration, String suffix) {
		Assert.hasText(suffix, "Suffix cannot be empty");
		this.configuration = configuration;
		this.suffix = suffix;
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	@Override
	public Path resolve(Path path) {
		if (path != null) {
			return path.suffix(suffix);
		}
		else {
			return path;
		}
	}

	@Override
	public void reset() {
	}

	@Override
	public void onApplicationEvent(AbstractStorageEvent event) {
		if (event instanceof FileWrittenEvent) {
			Path fromPath = ((FileWrittenEvent) event).getPath();
			String name = fromPath.getName();
			if (name.endsWith(suffix)) {
				Path toPath = new Path(fromPath.getParent(), name.substring(0, name.length() - suffix.length()));
				try {
					log.info("Renaming from " + fromPath + " to " + toPath + " with configuration " + configuration);
					FileSystem fs = fromPath.getFileSystem(configuration);
					fs.rename(fromPath, toPath);
				}
				catch (IOException e) {
					log.error("Error renaming file", e);
				}
			}
		}
	}

	/**
	 * Sets the suffix.
	 * 
	 * @param suffix the new suffix
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Sets the configuration.
	 * 
	 * @param configuration the new configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

}
