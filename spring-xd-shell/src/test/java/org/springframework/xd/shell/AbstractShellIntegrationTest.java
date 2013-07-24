/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.xd.dirt.server.AdminMain;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.stream.StreamServer;
import org.springframework.xd.shell.command.StreamCommandTests;

/**
 * Superclass for performing integration tests of spring-xd shell commands.
 * 
 * JUnit's BeforeClass and AfterClass annotations are used to start and stop the XDAdminServer
 * in local mode with the default store configured to use in-memory storage.  
 * 
 * Note: This isn't ideal as it takes significant time to startup the embedded XDContainer/tomcat and we should do this
 * once across all tests.
 * 
 * Note: The shutdown method is not successfully shutting down the embedded tomcat server..
 * 
 * @author Mark Pollack
 *
 */
public abstract class AbstractShellIntegrationTest {

	private static final Log logger = LogFactory.getLog(AbstractShellIntegrationTest.class);

	private static StreamServer server;
	private static JLineShellComponent shell;
	
	@BeforeClass
	public static void startUp() throws InterruptedException {
		AdminOptions opts = AdminMain.parseOptions(new String[] {"--httpPort", "8080", "--transport", "local", "--store", "memory", "--disableJmx", "true"});
		server = AdminMain.launchStreamServer(opts);
		
		//Start up the shell
		Bootstrap bootstrap = new Bootstrap();		
		shell = bootstrap.getJLineShellComponent();
	}
	
	@AfterClass
	public static void shutdown() {
		logger.info("Stopping StreamServer");
		System.out.println("Stopping StreamServer");
		
		//Note: This hangs on tomcat's stopInternal method waiting on a future for a child container to be destroyed.... 
		//      Not clear how to correctly stop the container.
		//      Maybe use cargo to start/stop the container- http://cargo.codehaus.org/Functional+testing
		server.stop();
		DirectFieldAccessor dfa = new DirectFieldAccessor(server);
		((XmlWebApplicationContext)dfa.getPropertyValue("webApplicationContext")).destroy();
		
		logger.info("Stopping XD Shell");
		System.out.println("Stopping XD Shell");
		shell.stop();
	}

	public static JLineShellComponent getShell() {
		return shell;
	}

	
}
