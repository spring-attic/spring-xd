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

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.support.ModuleUtils;

/**
 * A {@link SimpleModule} configured by an @Configuration class.
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
		String[] basePackages = basePackages(moduleDefinition, this.getClassLoader());
		Assert.notEmpty(basePackages, String.format("%s property does not exist or does not contain a value for " +
				"module" +
				" %s.", BASE_PACKAGES, moduleDefinition.toString()));
		addListener(new JavaConfigModuleListener(basePackages));
	}

	public static String[] basePackages(SimpleModuleDefinition moduleDefinition, ClassLoader moduleClassLoader) {
		Resource propertiesFile = ModuleUtils.locateModuleResource(moduleDefinition, moduleClassLoader,".properties");
		Assert.notNull(propertiesFile, "required module properties not found.");

		Properties properties = new Properties();
		try {
			properties.load(propertiesFile.getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException(String.format("Unable to read module properties for %s:%s",
					moduleDefinition.getName(), moduleDefinition.getType()), e);
		}

		String basePackageNames = properties.getProperty(BASE_PACKAGES);
		return StringUtils.commaDelimitedListToStringArray(basePackageNames);
	}



	static class JavaConfigModuleListener implements ApplicationListener<ApplicationPreparedEvent> {
		private final String[] basePackages;

		public JavaConfigModuleListener(String ... basePackages) {
			this.basePackages = basePackages;
		}
		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext)event.getApplicationContext();
			context.scan(basePackages);
		}
	}
}
