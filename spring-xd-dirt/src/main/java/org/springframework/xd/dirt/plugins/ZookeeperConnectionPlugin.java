/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.xd.dirt.plugins;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.core.Module;

/**
 * Supplies plugins with a reference to the Spring XD {@link ZooKeeperConnection}
 *
 * @author Marius Bogoevici
 */
public class ZookeeperConnectionPlugin extends AbstractPlugin {

	public static final String XD_ZOOKEEPER_CLIENT = "xd.zookeeper.client";

	@Override
	public boolean supports(Module module) {
		return true;
	}

	@Override
	public void preProcessModule(Module module) {
		module.addListener(new ApplicationListener<ApplicationPreparedEvent>() {
			@Override
			public void onApplicationEvent(ApplicationPreparedEvent event) {
				ZooKeeperConnection bean = getApplicationContext().getBean(ZooKeeperConnection.class);
				ConfigurableListableBeanFactory moduleBeanFactory = event.getApplicationContext().getBeanFactory();
				moduleBeanFactory.registerSingleton(XD_ZOOKEEPER_CLIENT, bean.getClient());
			}
		});
	}

}
