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

import static java.util.Arrays.asList;

import java.util.Properties;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.util.StringUtils;
import org.springframework.yarn.boot.cli.YarnClusterCreateCommand;

/**
 * A custom {@link YarnClusterCreateCommand} adding option for defining
 * a groups for XD's containers.
 *
 * @author Janne Valkealahti
 */
public class XdYarnClusterCreateCommand extends YarnClusterCreateCommand {

	public XdYarnClusterCreateCommand() {
		super(new CustomClusterCreateOptionHandler());
	}

	private static class CustomClusterCreateOptionHandler extends ClusterCreateOptionHandler {

		private OptionSpec<String> containerGroupsOption;

		@Override
		protected void options() {
			super.options();
			this.containerGroupsOption = option(asList("container-groups", "g"), "Container groups").withOptionalArg();
		}

		@Override
		protected Properties getExtraProperties(OptionSet options) {
			String containerGroups = options.valueOf(containerGroupsOption);
			Properties props = new Properties();
			if (StringUtils.hasText(containerGroups)) {
				props.put("containerGroups", containerGroups);
			}
			return props;
		}

	}

}
