
package org.springframework.xd.dirt.boot;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;

public class SingleNodeApplication {

	private static void setUpControlChannels(ApplicationContext adminContext,
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

	public static void main(String[] args) {

		SpringApplicationBuilder admin = new SpringApplicationBuilder(
				ParentConfiguration.class).profiles("adminServer", "singleNode").child(
				AdminServerApplication.class);
		admin.run(args);

		SpringApplicationBuilder container = admin
				.sibling(LauncherApplication.class).profiles("node", "singleNode").web(false);
		container.run(args);

		setUpControlChannels(admin.context(), container.context());

	}
}
