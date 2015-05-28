/*
 * Copyright 2014 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleUtils;

/**
 * A {@link SimpleModule} configured by an @Configuration class.
 *
 * @author David Turanski
 */
public class JavaConfiguredModule extends SimpleModule {

	private static final String BASE_PACKAGES = "base_packages";


	public JavaConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties) {
		super(descriptor, deploymentProperties);
	}

	public JavaConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			ClassLoader classLoader, ModuleOptions moduleOptions) {
		super(descriptor, deploymentProperties, classLoader, moduleOptions);
	}

	@Override
	protected void configureModuleApplicationContext(SimpleModuleDefinition moduleDefinition) {
		String[] basePackages = basePackages(moduleDefinition);
		Assert.notEmpty(basePackages, String.format("%s property does not exist or does not contain a value for " +
				"module" +
				" %s.", BASE_PACKAGES, moduleDefinition.toString()));
		addListener(new JavaConfigModuleListener(basePackages));
		addListener(new JavaConfigValidationListener(moduleDefinition, basePackages));

	}

	public static String[] basePackages(SimpleModuleDefinition moduleDefinition) {

		Properties properties = ModuleUtils.loadModuleProperties(moduleDefinition);
		//Assert.notNull(propertiesFile, "required module properties not found.");
		if (properties == null) {
			return new String[0];
		}


		String basePackageNames = properties.getProperty(BASE_PACKAGES);
		return StringUtils.commaDelimitedListToStringArray(basePackageNames);
	}


	static class JavaConfigModuleListener implements ApplicationListener<ApplicationPreparedEvent> {

		private final String[] basePackages;

		public JavaConfigModuleListener(String... basePackages) {
			this.basePackages = basePackages;
		}

		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) event
					.getApplicationContext();
			context.scan(basePackages);

		}
	}

	static class JavaConfigValidationListener implements ApplicationListener<ContextRefreshedEvent> {

		private final ModuleDefinition moduleDefinition;

		private final String[] basePackages;

		public JavaConfigValidationListener(SimpleModuleDefinition moduleDefinition, String[] basePackages) {
			this.basePackages = Arrays.copyOf(basePackages, basePackages.length);
			this.moduleDefinition = moduleDefinition;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ApplicationContext context = event.getApplicationContext();
			Map<String, Object> moduleConfiguration = context.getBeansWithAnnotation(Configuration.class);
			boolean found = false;
			if (!CollectionUtils.isEmpty(moduleConfiguration)) {
				for (String pkg : basePackages) {
					if (found) {
						break;
					}
					for (Object obj : moduleConfiguration.values()) {
						if (obj.getClass().getName().startsWith(pkg)) {
							found = true;
						}
					}
				}
			}
			if (!found) {
				throw new RuntimeException(String.format(
						"Unable to find a module @Configuration class in base_packages: %s for module %s:%s",
						StringUtils.arrayToCommaDelimitedString(basePackages),
						moduleDefinition.getName(), moduleDefinition.getType()
						));
			}
		}
	}
}
