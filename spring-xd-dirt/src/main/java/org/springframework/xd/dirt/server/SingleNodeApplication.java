
package org.springframework.xd.dirt.server;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public class SingleNodeApplication {

	private ConfigurableApplicationContext adminContext;

	private ConfigurableApplicationContext containerContext;

	public static void main(String[] args) {
		new SingleNodeApplication().run(args);
	}

	public SingleNodeApplication run(String... args) {

		SpringApplicationBuilder admin = new SpringApplicationBuilder(
				ParentConfiguration.class).profiles("adminServer", "single").child(
				AdminServerApplication.class);
		admin.run(args);

		SpringApplicationBuilder container = admin
				.sibling(LauncherApplication.class).profiles("node", "single").web(false);
		container.run(args);

		adminContext = admin.context();
		containerContext = container.context();
		// TODO: should be encapsulated (or maybe just deleted)
		LauncherApplication.publishContainerStarted(containerContext);
		setUpControlChannels(adminContext, containerContext);

		return this;

	}


	public ConfigurableApplicationContext getAdminContext() {
		return adminContext;
	}

	public ConfigurableApplicationContext getContainerContext() {
		return containerContext;
	}

	public void close() {
		if (containerContext != null) {
			containerContext.close();
		}
		if (adminContext != null) {
			adminContext.close();
			ApplicationContext parent = adminContext.getParent();
			if (parent instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) parent).close();
			}
		}
	}

	private void setUpControlChannels(ApplicationContext adminContext,
			ApplicationContext containerContext) {

		MessageChannel containerControlChannel = containerContext.getBean(
				"containerControlChannel", MessageChannel.class);
		SubscribableChannel deployChannel = adminContext.getBean(
				"deployChannel", SubscribableChannel.class);
		SubscribableChannel undeployChannel = adminContext.getBean(
				"undeployChannel", SubscribableChannel.class);

		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(containerControlChannel);
		handler.setComponentName("xd.local.control.bridge");
		deployChannel.subscribe(handler);
		undeployChannel.subscribe(handler);


	}

}
