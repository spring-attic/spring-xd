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

package org.springframework.data.hadoop.store;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.codec.CodecInfoAware;
import org.springframework.data.hadoop.store.naming.FileNamingStrategy;
import org.springframework.data.hadoop.store.rollover.RolloverSizeAware;
import org.springframework.data.hadoop.store.rollover.RolloverStrategy;

/**
 * Extension of {@code AbstractStorage} adding strategy interfaces handling file naming and rolloving.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class AbstractStrategiesStorage extends AbstractStorage implements StrategiesStorage {

	private FileNamingStrategy fileNamingStrategy;

	private RolloverStrategy rolloverStrategy;

	/** Handle for rollover strategy order to avoid casting */
	private RolloverSizeAware rolloverSizeAware;

	/**
	 * Instantiates a new abstract strategies storage.
	 * 
	 * @param configuration the hadoop configuration
	 * @param basePath the base path
	 * @param codec the codec info
	 */
	public AbstractStrategiesStorage(Configuration configuration, Path basePath, CodecInfo codec) {
		super(configuration, basePath, codec);
	}

	/**
	 * Sets the file naming strategy.
	 * 
	 * @param fileNamingStrategy the new file naming strategy
	 */
	@Override
	public void setFileNamingStrategy(FileNamingStrategy fileNamingStrategy) {
		this.fileNamingStrategy = fileNamingStrategy;
		if (fileNamingStrategy instanceof CodecInfoAware) {
			((CodecInfoAware) fileNamingStrategy).setCodecInfo(getCodec());
		}
	}

	/**
	 * Sets the rollover strategy.
	 * 
	 * @param rolloverStrategy the new rollover strategy
	 */
	@Override
	public void setRolloverStrategy(RolloverStrategy rolloverStrategy) {
		this.rolloverStrategy = rolloverStrategy;
		if (rolloverStrategy instanceof RolloverSizeAware) {
			rolloverSizeAware = (RolloverSizeAware) rolloverStrategy;
		}
		else {
			rolloverSizeAware = null;
		}
	}

	/**
	 * Report {@code RolloverSizeAware} rollover interface about a new file size.
	 * 
	 * @param size the size
	 */
	@Override
	public void reportSizeAware(long size) {
		if (rolloverSizeAware != null) {
			rolloverSizeAware.setSize(size);
		}
	}

	/**
	 * Handle rollover.
	 * 
	 * @param holder the holder
	 * @return true, if rollover should happen
	 */
	@Override
	public boolean checkStrategies() {
		if (rolloverStrategy == null) {
			return false;
		}

		boolean hasRolled = rolloverStrategy.hasRolled();
		if (hasRolled) {
			rolloverStrategy.reset();
			if (fileNamingStrategy != null) {
				fileNamingStrategy.reset();
			}
		}
		return hasRolled;
	}

	/**
	 * Gets the resolved path.
	 * 
	 * @return the resolved path
	 */
	@Override
	protected Path getResolvedPath() {
		return fileNamingStrategy != null ? fileNamingStrategy.resolve(super.getResolvedPath())
				: super.getResolvedPath();
	}

}
