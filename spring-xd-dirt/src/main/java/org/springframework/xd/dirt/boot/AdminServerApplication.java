
package org.springframework.xd.dirt.boot;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.rest.RestConfiguration;

@Configuration
@EnableAutoConfiguration
@ImportResource("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT
		+ "admin-server.xml")
@Import(RestConfiguration.class)
public class AdminServerApplication {

	private static final String PARENT_CONTEXT = "classpath:"
			+ XDContainer.XD_INTERNAL_CONFIG_ROOT + "xd-global-beans.xml";

	public static void main(String[] args) {
		new SpringApplicationBuilder(PARENT_CONTEXT).profiles("adminServer")
				.defaultArgs("--transport=redis")
				.child(AdminServerApplication.class).run(args);
	}
}
