/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.yarn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;
import org.springframework.core.io.FileSystemResource;

/**
 * A custom command for listing the http ports used by admin servers.
 *
 * @author Thomas Risberg
 */
@SuppressWarnings("unchecked")
public class XdYarnAdminInfoCommand extends AbstractCommand {

	private static final String XD_YARN_ADMIN_INFO_COMMAND_NAME = "admininfo";
	private static final String XD_YARN_ADMIN_INFO_COMMAND_DESC = "List http host and port info for the admin servers";

	public static final String ZK_NAMESPACE = "zk.namespace";
	public static final String ZK_CLIENT_CONNECT = "zk.client.connect";

	public static final String XD_ADMINS = "admins";

	private String zkNamespace = "xd";
	private String zkClientConnect = "localhost:2181";

	private ZooKeeper zk;

	/**
	 * Call super constructor for new {@link org.springframework.boot.cli.command.AbstractCommand} instance.
	 */
	public XdYarnAdminInfoCommand() {
		super(XD_YARN_ADMIN_INFO_COMMAND_NAME, XD_YARN_ADMIN_INFO_COMMAND_DESC);
	}

	@Override
	public ExitStatus run(String... args) throws InterruptedException {
		try {
			init();
			Log.info("Admins: " + getAdmins());
		}
		catch (IOException e) {
			Log.error("IOException accessing admin info: " + e.getMessage());
		}
		catch (KeeperException e) {
			Log.error("KeeperException accessing admin info: " + e.getMessage());
		}
		finally {
			close();
		}
		return ExitStatus.OK;
	}

	private void init() throws IOException {
		YamlPropertiesFactoryBean ypfb = new YamlPropertiesFactoryBean();
		ypfb.setResources(new FileSystemResource(System.getProperty("spring.config.location")));
		Properties conf = ypfb.getObject();
		if (conf.containsKey(ZK_NAMESPACE)) {
			zkNamespace = (String) conf.get(ZK_NAMESPACE);
		}
		if (conf.containsKey(ZK_CLIENT_CONNECT)) {
			zkClientConnect = (String) conf.get(ZK_CLIENT_CONNECT);
		}
		zk = new ZooKeeper(zkClientConnect, 5000,
				new Watcher() {
					@Override
					public void process(WatchedEvent event) {
					}
				});
	}

	private void close() throws InterruptedException {
		if (zk != null) {
			zk.close();
		}
	}

	private List<String> getAdmins() throws KeeperException {
		String adminsPath = "/" + zkNamespace + "/" + XD_ADMINS;
		List<String> results = new ArrayList<String>();
		try {
			Stat stat = zk.exists(adminsPath, false);
			if (stat != null) {
				List<String> admins = zk.getChildren(adminsPath, false);
				for (String admin : admins) {
					byte[] data = zk.getData(adminsPath + "/" + admin, false, null);
					try {
						HashMap<String,Object> dataValues =
								new ObjectMapper().readValue(new String(data), HashMap.class);
						results.add("http://" + dataValues.get("host") + ":" + dataValues.get("port"));
					} catch (IOException e) {
						Log.error("IOException while retrieving data for " + admin + ": " + e.getMessage());
					}
				}
			}
		} catch (InterruptedException ignore) {}
		return results;
	}
}
