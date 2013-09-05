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

package org.springframework.xd.yarn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.AdminServer;
import org.springframework.yarn.YarnSystemConstants;
import org.springframework.yarn.am.CommandLineAppmasterRunner;
import org.springframework.yarn.am.track.UrlAppmasterTrackService;

/**
 * Custom command line runner for Spring XD Yarn Application Master.
 * <p>
 * This is needed order to manually setup Web Application Context needed my XD Admin build-in mvc layer.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnAppmasterRunner extends CommandLineAppmasterRunner {

	private final static Log log = LogFactory.getLog(XdYarnAppmasterRunner.class);

	@Override
	protected ConfigurableApplicationContext getChildApplicationContext(
			String configLocation, ConfigurableApplicationContext parent) {

		log.info("Using XD_TRANSPORT=" + System.getProperty("XD_TRANSPORT"));
		log.info("Using XD_STORE=" + System.getProperty("XD_STORE"));
		log.info("Using XD_HOME=" + System.getProperty("XD_HOME"));

		XmlWebApplicationContext context = new XmlWebApplicationContext();
		context.setParent(parent);
		context.setConfigLocation("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "admin-server.xml");

		final AdminServer server = new AdminServer(context);
		server.run();

		// context is already refreshed so just register singleton
		// not really a proper way to do it but serves the demo purpose
		// if streamserver could choose free port then below would
		// make much more sense
		parent.getBeanFactory().registerSingleton(
				YarnSystemConstants.DEFAULT_ID_AMTRACKSERVICE,
				new UrlAppmasterTrackService("http://localhost:" + server.getLocalPort()));

		context.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {

			@Override
			public void onApplicationEvent(ContextClosedEvent event) {
				server.stop();
			}

		});

		return context;
	}

	public static void main(String[] args) {
		new XdYarnAppmasterRunner().doMain(args);
	}

}
