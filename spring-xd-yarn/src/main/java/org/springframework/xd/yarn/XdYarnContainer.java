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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.launcher.ContainerLauncher;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.yarn.container.AbstractYarnContainer;

/**
 * Custom Spring XD Yarn container handling XD container launch operation.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnContainer extends AbstractYarnContainer implements ApplicationContextAware {

	private static final Log log = LogFactory.getLog(XdYarnContainer.class);

	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Override
	protected void runInternal() {
		log.info("XdContainer internal run starting, setting up XD container launcher");
		log.info("Using XD_HOME=" + getEnvironment("XD_HOME"));
		log.info("XdContainer context=" + context);
		log.info("XdContainer parent context=" + context.getParent());

		ContainerOptions options = new ContainerOptions();
		String[] args = new String[] {
			"--xdHomeDir",
			getEnvironment("XD_HOME")
		};
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			log.error("Error parsing options", e);
		}

		log.info("Going to launch Spring XD specific XDContainer");
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		XDContainer container = launcher.launch(options);

		log.info("XdContainer launched id=" + container.getId() + " jvm=" + container.getJvmName());
	}

	@Override
	public boolean isWaitCompleteState() {
		return true;
	}
}
