/*
 * Copyright 2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.Container;

import org.springframework.yarn.am.cluster.ContainerCluster;
import org.springframework.yarn.am.cluster.ManagedContainerClusterAppmaster;

/**
 * Custom application master handling XD containers grouping setting
 * for launch commands.
 *
 * @author Janne Valkealahti
 * 
 */
public class XdAppmaster extends ManagedContainerClusterAppmaster {

	@Override
	protected List<String> onContainerLaunchCommands(Container container, ContainerCluster cluster,
			List<String> commands) {

		// only modify container so assume presence of -Dspring.application.name=container
		if (findPosition(commands, "-Dspring.application.name=container") < 0) {
			return commands;
		}

		ArrayList<String> list = new ArrayList<String>(commands);
		Map<String, Object> extraProperties = cluster.getExtraProperties();
		if (extraProperties != null && extraProperties.containsKey("containerGroups")) {
			int position = findPosition(commands, "-Dxd.container.groups=");
			String value = "-Dxd.container.groups=" + cluster.getExtraProperties().get("containerGroups");
			if (position < 0) {
				list.add(Math.max(list.size() - 3, 0), value);
			}
			else {
				list.set(position, value);
			}
		}
		int memory = container.getResource().getMemory();
		int virtualCores = container.getResource().getVirtualCores();
		list.add(Math.max(list.size() - 3, 0), "-Dxd.container.memory=" + memory);
		list.add(Math.max(list.size() - 3, 0), "-Dxd.container.virtualCores=" + virtualCores);
		return list;
	}

	private static int findPosition(List<String> list, String text) {
		for (int i = 0; i < list.size(); i += 1) {
			if (list.get(i).indexOf(text) != -1) {
				return i;
			}
		}
		return -1;
	}

}
