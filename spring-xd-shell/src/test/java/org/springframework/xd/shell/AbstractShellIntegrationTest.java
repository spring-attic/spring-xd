package org.springframework.xd.shell;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.xd.dirt.server.AdminMain;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.stream.StreamServer;

public abstract class AbstractShellIntegrationTest {

	private static StreamServer server;
	@BeforeClass
	public static void startUp() {
		AdminOptions opts = AdminMain.parseOptions(new String[] {"--httpPort", "0", "--transport", "local", "--store", "redis", "--disableJmx", "true"});
		server = AdminMain.launchStreamServer(opts);
	}
	
	@AfterClass
	public static void shutdown() {
		server.stop();
		DirectFieldAccessor dfa = new DirectFieldAccessor(server);
		((XmlWebApplicationContext)dfa.getPropertyValue("webApplicationContext")).destroy();
	}
	
}
