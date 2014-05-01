/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * Abstract {@link Plugin} that contains no-op and common implementing methods.
 * 
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * 
 */
public abstract class AbstractPlugin implements Plugin, Ordered, ApplicationContextAware {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private ApplicationContext applicationContext;

	protected static final String PLUGIN_CONTEXT_CONFIG_ROOT = ConfigLocations.XD_CONFIG_ROOT + "plugins/";

	@Override
	public void preProcessModule(Module module) {
	}

	@Override
	public void postProcessModule(Module module) {
	}

	@Override
	public void removeModule(Module module) {
	}

	@Override
	public void beforeShutdown(Module module) {
	}

	@Override
	public abstract boolean supports(Module module);

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
