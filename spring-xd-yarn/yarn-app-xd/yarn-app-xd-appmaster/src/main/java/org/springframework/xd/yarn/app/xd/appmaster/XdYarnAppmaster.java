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

package org.springframework.xd.yarn.app.xd.appmaster;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.xd.yarn.app.xd.PollingTaskSupport;
import org.springframework.yarn.YarnSystemConstants;
import org.springframework.yarn.am.StaticAppmaster;
import org.springframework.yarn.am.track.UrlAppmasterTrackService;
import org.springframework.yarn.boot.support.SpringYarnAppmasterProperties;
import org.springframework.yarn.support.NetworkUtils;

/**
 * Custom application master for XD.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnAppmaster extends StaticAppmaster implements ApplicationListener<ApplicationEvent> {

	private final static Log log = LogFactory.getLog(XdYarnAppmaster.class);

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private SpringYarnAppmasterProperties syap;

	private volatile StartDelayPoller poller;

	@Override
	public void submitApplication() {
		log.info("Delaying appmaster submit and registration order to wait embedded web port");
	}

	@Override
	protected void doStop() {
		if (poller != null) {
			poller.stop();
		}
		poller = null;
		super.doStop();
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {

		Object source = event.getSource();
		if (source instanceof AnnotationConfigEmbeddedWebApplicationContext) {
			poller = new StartDelayPoller(getTaskScheduler(), getTaskExecutor(),
					((AnnotationConfigEmbeddedWebApplicationContext) source).getEmbeddedServletContainer());
			poller.init();
			poller.start();
		}
	}

	@Override
	public ContainerLaunchContext preLaunch(Container container, ContainerLaunchContext context) {
		Map<String, String> env = new HashMap<String, String>(context.getEnvironment());
		env.put("XD_HOME", "./" + syap.getContainerFile());
		context.setEnvironment(env);
		return context;
	}

	private void submitApplicationInternal(int port) {

		if (ctx instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) ctx).getBeanFactory().registerSingleton(
					YarnSystemConstants.DEFAULT_ID_AMTRACKSERVICE,
					new UrlAppmasterTrackService("http://" + NetworkUtils.getDefaultAddress() + ":" + port));
		}

		// TODO: should guard that CommandLineRunner have done
		// its job, otherwise appmaster is not fully initialized
		super.submitApplication();

		if (poller != null) {
			poller.stop();
		}
		poller = null;

	}

	private class StartDelayPoller extends PollingTaskSupport<Integer> {

		private final EmbeddedServletContainer embeddedServletContainer;

		private boolean done;

		public StartDelayPoller(TaskScheduler taskScheduler, TaskExecutor taskExecutor,
				EmbeddedServletContainer embeddedServletContainer) {
			super(taskScheduler, taskExecutor);
			this.embeddedServletContainer = embeddedServletContainer;
		}

		@Override
		protected Integer doPoll() {
			int port = embeddedServletContainer.getPort();
			if (log.isDebugEnabled()) {
				log.debug("Polling port from EmbeddedServletContainer port=" + port);
			}
			return port;
		}

		@Override
		protected void onPollResult(Integer result) {
			if (result > 0 && !done) {
				done = true;
				submitApplicationInternal(result);
			}
		}

	}

}
