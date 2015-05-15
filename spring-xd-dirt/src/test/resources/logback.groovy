/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.util.logging

import org.springframework.xd.dirt.util.logging.CustomLoggerConverter
// We highly recommended that you always add a status listener just
// after the last import statement and before all other statements
// NOTE - this includes logging configuration in the log and stacktraces in the event of errors
// statusListener(OnConsoleStatusListener)

// Emulates Log4j formatting
conversionRule("category", CustomLoggerConverter)

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%d{'HH:mm:ss,SSS'} %level{5} %thread %category{2} - %msg%n"
	}
}
root(INFO, ["STDOUT"])

logger("org.springframework", INFO)
logger("org.springframework.boot", INFO)
logger("org.springframework.beans.factory.config", ERROR)
logger("org.springframework.xd.dirt", INFO)
logger("org.springframework.xd.dirt.launcher", INFO)
logger("org.springframework.xd.dirt.launcher.RedisContainerLauncher", INFO)
logger("org.springframework.xd.dirt.util.XdConfigLoggingInitializer", INFO)
logger("org.springframework.integration", WARN)
logger("org.springframework.integration.x", WARN)
logger("org.springframework.retry", WARN)
logger("org.springframework.amqp", WARN)
logger("xd.sink", INFO)
logger("org.apache.zookeeper", INFO)
logger("org.apache.curator", INFO)

//This prevents the "Error:KeeperErrorCode = NodeExists" INFO messages
//logged by ZooKeeper when a parent node does not exist while
//invoking Curator's creatingParentsIfNeeded node builder.
logger("org.apache.zookeeper.server.PrepRequestProcessor", WARN)

// This prevents warning message during shutdown of the EmbeddedZookeeper//
// javax.management.InstanceNotFoundException: org.apache.ZooKeeperService:name0=StandaloneServer_port-1,name1=InMemoryDataTree
logger("org.apache.zookeeper.jmx.MBeanRegistry", ERROR)

//Prevent sftp source module's JCraft jsch info logging
logger("com.jcraft.jsch", ERROR)
