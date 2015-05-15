/*
 * Copyright 2011-2012 the original author or authors.
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

package org.springframework.xd.shell.hadoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.conf.Configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.shell.core.ExecutionProcessor;
import org.springframework.shell.event.ParseResult;

/**
 * Utility base class for components monitoring {@link Configuration} changes in order to update state.
 * 
 * @author Costin Leau
 * @author Jarred Li
 * @author Glenn Renfro
 */
public abstract class ConfigurationAware implements ApplicationListener<ConfigurationModifiedEvent>, ExecutionProcessor {

	protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	@Autowired
	private Configuration hadoopConfiguration;

	private boolean needToReinitialize = false;

	@Override
	public void onApplicationEvent(ConfigurationModifiedEvent event) {
		needToReinitialize = true;
	}

	@Override
	public ParseResult beforeInvocation(ParseResult invocationContext) {
		// check whether the Hadoop configuration has changed
		if (needToReinitialize) {
			try {
				this.needToReinitialize = !configurationChanged();
			}
			catch (Exception ex) {
				logUpdateError(ex);
			}
		}
		return invocationContext;
	}

	/**
	 * Called before invoking a command in case the configuration changed. Should return true if the change has been
	 * acknowledged, false otherwise.
	 * 
	 * Implementation are encouraged to update their name accordingly through {@link #failedComponentName()} to provide
	 * proper error messages.
	 * 
	 * @return true if the change has been acknowledged, false otherwise.
	 * @throws Exception ignored and causing the re-initialization to occur on the next call
	 * 
	 * @see #failedComponentName()
	 * @see #logUpdateError(Exception)
	 */
	protected abstract boolean configurationChanged() throws Exception;

	protected void logUpdateError(Exception ex) {
		LOG.error("Hadoop configuration changed but updating [" + failedComponentName() + "] failed; cause=" + ex);
	}

	protected String failedComponentName() {
		return getClass().getName();
	}

	@Override
	public void afterReturningInvocation(ParseResult invocationContext, Object result) {
		// no-op
	}

	@Override
	public void afterThrowingInvocation(ParseResult invocationContext, Throwable thrown) {
		// no-op
	}

	/**
	 * Gets the hadoop configuration.
	 * 
	 * @return the hadoopConfiguration
	 */
	public Configuration getHadoopConfiguration() {
		return hadoopConfiguration;
	}
}
