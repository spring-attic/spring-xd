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
import org.springframework.data.hadoop.store.StorageException;
import org.springframework.data.hadoop.store.event.AbstractStorageEvent;
import org.springframework.data.hadoop.store.event.FileWrittenEvent;
import org.springframework.util.StringUtils;

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

	private final static String DEFAULT_SUFFIX = ".tmp";

	private String suffix;

	private String prefix;

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
		this(configuration, DEFAULT_SUFFIX);
	}

	/**
	 * Instantiates a new renaming file naming strategy.
	 * 
	 * @param configuration the configuration
	 * @param suffix the suffix
	 */
	public RenamingFileNamingStrategy(Configuration configuration, String suffix) {
		this(configuration, suffix, null);
	}

	/**
	 * Instantiates a new renaming file naming strategy.
	 * 
	 * @param configuration the configuration
	 * @param suffix the suffix
	 * @param prefix the prefix
	 */
	public RenamingFileNamingStrategy(Configuration configuration, String suffix, String prefix) {
		this.configuration = configuration;
		this.suffix = suffix;
		this.prefix = prefix;
	}

	@Override
	public int getOrder() {
		// we potentially modify final path and filename,
		// so order needs to be a last one
		return LOWEST_PRECEDENCE;
	}

	@Override
	public Path resolve(Path path) {
		if (path != null) {
			String name = (StringUtils.hasText(prefix) ? prefix : "") + path.getName()
					+ (StringUtils.hasText(suffix) ? suffix : "");
			return new Path(path.getParent(), name);
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
			Path path = ((FileWrittenEvent) event).getPath();
			if (path != null) {
				renameFile(path);
			}
		}
	}

	/**
	 * Rename file using prefix and suffix settings.
	 * 
	 * @param path the path to rename
	 */
	protected void renameFile(Path path) {
		String name = path.getName();
		if (StringUtils.startsWithIgnoreCase(name, prefix)) {
			name = name.substring(prefix.length());
		}
		if (StringUtils.endsWithIgnoreCase(name, suffix)) {
			name = name.substring(0, name.length() - suffix.length());
		}
		Path toPath = new Path(path.getParent(), name);
		try {
			FileSystem fs = path.getFileSystem(configuration);
			if (!fs.rename(path, toPath)) {
				throw new StorageException("Failed renaming from " + path + " to " + toPath
						+ " with configuration " + configuration);
			}
		}
		catch (IOException e) {
			log.error("Error renaming file", e);
			throw new StorageException("Error renaming file", e);
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
	 * Sets the prefix.
	 * 
	 * @param prefix the new prefix
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
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
