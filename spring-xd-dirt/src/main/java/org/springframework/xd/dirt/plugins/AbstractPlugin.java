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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * Abstract {@link Plugin} that contains no-op and common implementing methods.
 * 
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * 
 */
public abstract class AbstractPlugin implements Plugin, Ordered {

	protected final Log logger = LogFactory.getLog(getClass());

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

	protected MessageBus findMessageBus(Module module) {
		MessageBus messageBus = null;
		try {
			messageBus = module.getComponent(MessageBus.class);
		}
		catch (Exception e) {
			logger.error("No MessageBus in context, cannot wire/unwire channels: " + e.getMessage());
		}
		return messageBus;
	}

	@Override
	public abstract boolean supports(Module module);

	@Override
	public int getOrder() {
		return 0;
	}

}
