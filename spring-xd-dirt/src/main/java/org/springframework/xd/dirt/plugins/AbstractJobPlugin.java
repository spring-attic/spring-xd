/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;


/**
 * Abstract class that extends {@link AbstractMessageBusBinderPlugin} and has common implementation methods related to
 * job plugins.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 */
public class AbstractJobPlugin extends AbstractMessageBusBinderPlugin {

	public AbstractJobPlugin(MessageBus messageBus) {
		super(messageBus);
	}

	public static String getJobChannelName(String jobName) {
		return JOB_CHANNEL_PREFIX + jobName;
	}

	@Override
	protected String getInputChannelName(Module module) {
		String group = module.getDescriptor().getGroup();
		return getJobChannelName(group);
	}

	@Override
	protected String getOutputChannelName(Module module) {
		throw new UnsupportedOperationException("Job module doesn't have output channel set.");
	}

	@Override
	protected String buildTapChannelName(Module module) {
		throw new UnsupportedOperationException(
				"Tap on job module is not valid as job module doesn't have an output channel.");
	}

	@Override
	public boolean supports(Module module) {
		return ((module.shouldBind()) && module.getType() == ModuleType.job);
	}
}
