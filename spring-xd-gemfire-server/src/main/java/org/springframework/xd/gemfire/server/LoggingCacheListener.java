/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.gemfire.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.RegionEvent;

/**
 * A simple cache listener that will log events.
 *
 * @author David Turanski
 */
public class LoggingCacheListener<K, V> implements CacheListener<K, V> {

	private static enum Level {
		FATAL, ERROR, WARN, INFO, DEBUG, TRACE
	}

	private final Level level;

	private static Logger logger = LoggerFactory.getLogger(LoggingCacheListener.class);

	public LoggingCacheListener() {
		this("DEBUG");
	}

	public LoggingCacheListener(String level) {
		try {
			this.level = Level.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level
					+ "'. The (case-insensitive) supported values are: "
					+ StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
	}

	@Override
	public void close() {
	}

	@Override
	public void afterCreate(EntryEvent<K, V> arg0) {
		logEvent("created entry " + arg0.getKey());
	}

	@Override
	public void afterDestroy(EntryEvent<K, V> arg0) {
		logEvent("destroyed entry " + arg0.getKey());
	}

	@Override
	public void afterInvalidate(EntryEvent<K, V> arg0) {
		logEvent("invalidated entry " + arg0.getKey());
	}

	@Override
	public void afterRegionClear(RegionEvent<K, V> arg0) {
		logEvent("region " + arg0.getRegion().getName() + " cleared");
	}

	@Override
	public void afterRegionCreate(RegionEvent<K, V> arg0) {
		logEvent("region " + arg0.getRegion().getName() + " created");
	}

	@Override
	public void afterRegionDestroy(RegionEvent<K, V> arg0) {
		logEvent("region " + arg0.getRegion().getName() + " destroyed");
	}

	@Override
	public void afterRegionInvalidate(RegionEvent<K, V> arg0) {
		logEvent("region " + arg0.getRegion().getName() + " invalidated");
	}

	@Override
	public void afterRegionLive(RegionEvent<K, V> arg0) {
		logEvent("region " + arg0.getRegion().getName() + " live");
	}

	@Override
	public void afterUpdate(EntryEvent<K, V> arg0) {
		logEvent("updated entry " + arg0.getKey());
	}

	private void logEvent(String logMessage) {
		switch (this.level) {
			case ERROR:
				if (logger.isErrorEnabled()) {
					logger.error(logMessage);
				}
				break;
			case WARN:
				if (logger.isWarnEnabled()) {
					logger.warn(logMessage);
				}
				break;
			case INFO:
				if (logger.isInfoEnabled()) {
					logger.info(logMessage);
				}
				break;
			case DEBUG:
				if (logger.isDebugEnabled()) {
					logger.debug(logMessage);
				}
				break;
			case TRACE:
				if (logger.isTraceEnabled()) {
					logger.trace(logMessage);
				}
				break;
		}
	}

}
