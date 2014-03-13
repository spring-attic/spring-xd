package org.springframework.xd.yarn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.yarn.client.YarnClient;

import java.util.ArrayList;
import java.util.List;

@EnableAutoConfiguration
public class ClientApp {
	public static void main(String[] args) {
		boolean install = false;
		boolean submit = false;
		List<String> appArgs = new ArrayList<String>();
		for (String arg : args) {
			if ("--install".equals(arg)) {
				install = true;
			} else {
				if ("--submit".equals(arg)) {
					submit = true;
				} else {
					appArgs.add(arg);
				}
			}
		}
		if (!submit && !install) {
			submit = true;
		}
		ConfigurableApplicationContext ctx =
				SpringApplication.run(ClientApp.class, appArgs.toArray(new String[appArgs.size()]));
		YarnClient client = ctx.getBean(YarnClient.class);
		if (install) {
			client.installApplication();
		}
		if (submit) {
			client.submitApplication(false);
		}
	}

}
