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

import org.springframework.boot.cli.command.Command;
import org.springframework.yarn.boot.cli.AbstractCli;
import org.springframework.yarn.boot.cli.YarnClusterDestroyCommand;
import org.springframework.yarn.boot.cli.YarnClusterInfoCommand;
import org.springframework.yarn.boot.cli.YarnClusterModifyCommand;
import org.springframework.yarn.boot.cli.YarnClusterStartCommand;
import org.springframework.yarn.boot.cli.YarnClusterStopCommand;
import org.springframework.yarn.boot.cli.YarnClustersInfoCommand;
import org.springframework.yarn.boot.cli.YarnKillCommand;
import org.springframework.yarn.boot.cli.YarnPushCommand;
import org.springframework.yarn.boot.cli.YarnPushedCommand;
import org.springframework.yarn.boot.cli.YarnSubmitCommand;
import org.springframework.yarn.boot.cli.YarnSubmittedCommand;
import org.springframework.yarn.boot.cli.YarnSubmittedCommand.SubmittedOptionHandler;
import org.springframework.yarn.boot.cli.shell.ShellCommand;

/**
 * Cli for controlling XD instances on an application master.
 *
 * @author Janne Valkealahti
 *
 */
public class ClientApplication extends AbstractCli {

	public static void main(String... args) {
		ClientApplication app = new ClientApplication();
		
		// command-line commands
		List<Command> cmdCommands = new ArrayList<Command>();
		cmdCommands.add(new YarnPushCommand());
		cmdCommands.add(new YarnPushedCommand());
		cmdCommands.add(new YarnSubmitCommand());
		cmdCommands.add(new YarnSubmittedCommand(new SubmittedOptionHandler("XD")));
		cmdCommands.add(new YarnKillCommand());
		cmdCommands.add(new YarnClustersInfoCommand());
		cmdCommands.add(new YarnClusterInfoCommand());
		cmdCommands.add(new XdYarnClusterCreateCommand());
		cmdCommands.add(new YarnClusterStartCommand());
		cmdCommands.add(new YarnClusterStopCommand());
		cmdCommands.add(new YarnClusterModifyCommand());
		cmdCommands.add(new YarnClusterDestroyCommand());		
		app.registerCommands(cmdCommands);

		// shell commands, push not supported in shell
		List<Command> shellCommands = new ArrayList<Command>();
		shellCommands.add(new YarnPushedCommand());
		shellCommands.add(new YarnSubmitCommand());
		shellCommands.add(new YarnSubmittedCommand(new SubmittedOptionHandler("XD")));
		shellCommands.add(new YarnKillCommand());
		shellCommands.add(new YarnClustersInfoCommand());
		shellCommands.add(new YarnClusterInfoCommand());
		shellCommands.add(new XdYarnClusterCreateCommand());
		shellCommands.add(new YarnClusterStartCommand());
		shellCommands.add(new YarnClusterStopCommand());
		shellCommands.add(new YarnClusterModifyCommand());
		shellCommands.add(new YarnClusterDestroyCommand());		
		app.registerCommand(new ShellCommand(shellCommands));
		
		app.doMain(args);
	}
	
	@Override
	protected String getMainCommandName() {
		return "xd-yarn";
	}

}
