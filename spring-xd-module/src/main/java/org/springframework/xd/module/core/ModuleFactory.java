/*
 *
 *  * Copyright 2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.module.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PrefixNarrowingModuleOptions;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 *
 * Creates a {@link Module} instance and configures its application context using resources found under the resource location defined in the {@link ModuleDefinition}.
 *
 * @author David Turanski
 */
public class ModuleFactory implements BeanClassLoaderAware {
	private static Log log = LogFactory.getLog(ModuleFactory.class);

	private volatile ClassLoader classLoader;

	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	/**
	 *
	 * @param moduleOptionsMetadataResolver Used to bind configured {@link ModuleOptions} to {@link Module} instances
	 */
	public ModuleFactory(ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		Assert.notNull(moduleOptionsMetadataResolver, "'moduleOptionsMetadataResolver' cannot be null");
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	/**
	 * Create a new {@link Module} instance.
	 * @param moduleDescriptor contains the module's runtime configuration (required)
	 * @param deploymentProperties contains deployment properties (may be null)
	 * @return the module instance
	 */
	public Module newInstance(ModuleDescriptor moduleDescriptor, ModuleDeploymentProperties deploymentProperties) {
		ModuleOptions moduleOptions = this.safeModuleOptionsInterpolate(moduleDescriptor);
		Module module = createAndConfigureModuleInstance(moduleDescriptor, moduleOptions, deploymentProperties);
		return module;
	}

	/**
	 * Configure the application context from the resource. This is required to load any beans that are installed for the module.
	 * @param module
	 */
	private void configureModuleApplicationContext(Module module) {
		ModuleDefinition moduleDefinition = module.getDescriptor().getModuleDefinition();
		//todo: change this when ModuleDefinition is refactored.
		Resource resource = moduleDefinition.getResource();
		if (log.isInfoEnabled()) {
			log.info("configuring module " + module.getType() + ":" + module.getName() + " from module definition resource " + module.getDescriptor().getModuleDefinition().getResource());
		}
		if (resource != null && resource.exists() && resource.isReadable()) {
			module.addSource(moduleDefinition.getResource());
		}
	}

	/**
	 *
	 * @param moduleDescriptor
	 * @param moduleOptions
	 * @param deploymentProperties
	 * @return
	 */
	private Module createAndConfigureModuleInstance(ModuleDescriptor moduleDescriptor, ModuleOptions moduleOptions, ModuleDeploymentProperties deploymentProperties) {
		Module module = moduleDescriptor.isComposed() ? createComposedModule(moduleDescriptor, moduleOptions, deploymentProperties) :
				createSimpleModule(moduleDescriptor, moduleOptions, deploymentProperties);
		configureModuleApplicationContext(module);
		return module;
	}

	/**
	 * Create a simple module based on the provided {@link ModuleDescriptor}, {@link org.springframework.xd.module.options.ModuleOptions}, and
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param moduleDescriptor descriptor for the composed module
	 * @param moduleOptions module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 *
	 * @return new simple module instance
	 *
	 */
	private Module createSimpleModule(ModuleDescriptor moduleDescriptor, ModuleOptions moduleOptions,
			ModuleDeploymentProperties deploymentProperties) {
		if (log.isInfoEnabled()) {
			log.info("creating simple module " + moduleDescriptor);
		}
		ModuleDefinition definition = moduleDescriptor.getModuleDefinition();
		ClassLoader moduleClassLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), this.classLoader);
		return new SimpleModule(moduleDescriptor, deploymentProperties, moduleClassLoader, moduleOptions);
	}

	/**
	 * Create a composed module based on the provided {@link ModuleDescriptor}, {@link org.springframework.xd.module.options.ModuleOptions}, and
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param compositeDescriptor descriptor for the composed module
	 * @param options module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 *
	 * @return new composed module instance
	 *
	 * @see ModuleDescriptor#isComposed
	 */
	private Module createComposedModule(ModuleDescriptor compositeDescriptor,
			ModuleOptions options, ModuleDeploymentProperties deploymentProperties) {
		List<ModuleDescriptor> children = compositeDescriptor.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");
		if (log.isInfoEnabled()) {
			log.info("creating complex module " + compositeDescriptor);
		}

		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDescriptor moduleDescriptor : children) {
			ModuleOptions moduleOptions = new PrefixNarrowingModuleOptions(options, moduleDescriptor.getModuleName());
			// due to parser results being reversed, we add each at index 0
			// todo: is it right to pass the composite deploymentProperties here?
			childrenModules.add(0, createAndConfigureModuleInstance(moduleDescriptor, moduleOptions, deploymentProperties));
		}
		return new CompositeModule(compositeDescriptor, deploymentProperties, childrenModules);
	}

	/**
	 * Takes a request and returns an instance of {@link ModuleOptions} bound with the request parameters. Binding is
	 * assumed to not fail, as it has already been validated on the admin side.
	 *
	 * @param descriptor module descriptor for which to bind request parameters
	 *
	 * @return module options bound with request parameters
	 */
	private ModuleOptions safeModuleOptionsInterpolate(ModuleDescriptor descriptor) {
		Map<String, String> parameters = descriptor.getParameters();
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(descriptor.getModuleDefinition());
		try {
			return moduleOptionsMetadata.interpolate(parameters);
		}
		catch (BindException e) {
			// Can't happen as parser should have already validated options
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
}
