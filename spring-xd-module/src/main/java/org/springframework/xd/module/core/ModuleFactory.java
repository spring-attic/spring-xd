/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PrefixNarrowingModuleOptions;
import org.springframework.xd.module.options.ModuleUtils;

/**
 * Determines the type of {@link Module} to create from the Module's metadata and creates a module instance. Also,
 * resolves {@link org.springframework.xd.module.options.ModuleOptions} in the process.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class ModuleFactory implements BeanClassLoaderAware {

	private static Logger log = LoggerFactory.getLogger(ModuleFactory.class);

	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	private volatile ClassLoader parentClassLoader = ModuleFactory.class.getClassLoader();

	/**
	 * This key is used by the module to define the execution framework(spark streaming, reactor etc.,) to be used when
	 * deploying it.
	 */
	public static final String MODULE_EXECUTION_FRAMEWORK_KEY = "moduleExecutionFramework";

	/**
	 * @param moduleOptionsMetadataResolver Used to bind configured {@link ModuleOptions} to {@link Module} instances
	 */
	public ModuleFactory(ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		Assert.notNull(moduleOptionsMetadataResolver, "'moduleOptionsMetadataResolver'" + " cannot be null");
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	/**
	 * Create a new {@link org.springframework.xd.module.core.SimpleModule} or
	 * {@link org.springframework.xd.module.core.CompositeModule} instance from inspecting the
	 * {@link org.springframework.xd.module.ModuleDescriptor}, particularly the descriptor's
	 * {@link org.springframework.xd.module.ModuleDefinition}.
	 *
	 * @param moduleDescriptor contains the module's runtime configuration (required)
	 * @param deploymentProperties contains deployment properties (may be null)
	 * @return the module instance
	 */
	public Module createModule(ModuleDescriptor moduleDescriptor, ModuleDeploymentProperties deploymentProperties) {
		ModuleOptions moduleOptions = this.safeModuleOptionsInterpolate(moduleDescriptor);
		Module module = createAndConfigureModuleInstance(moduleDescriptor, moduleOptions, deploymentProperties);
		return module;
	}


	/**
	 * Creates and configures a {@link org.springframework.xd.module.core.Module} after resolving {@link
	 * org.springframework.xd.module.options.ModuleOptions}. createComposedModule() calls this for each component
	 * module.
	 *
	 * @param moduleDescriptor
	 * @param moduleOptions
	 * @param deploymentProperties
	 * @return the module instance
	 */
	private Module createAndConfigureModuleInstance(ModuleDescriptor moduleDescriptor, ModuleOptions moduleOptions,
			ModuleDeploymentProperties deploymentProperties) {
		Module module = moduleDescriptor.isComposed() ?
				createCompositeModule(moduleDescriptor, moduleOptions, deploymentProperties) :
				createSimpleModule(moduleDescriptor, moduleOptions, deploymentProperties);
		return module;
	}

	/**
	 * Create a simple module based on the provided {@link ModuleDescriptor}, {@link ModuleOptions}, and {@link ModuleDeploymentProperties}.
	 *
	 * @param moduleDescriptor descriptor for the composed module
	 * @param moduleOptions module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 * @return new simple module instance
	 */
	private Module createSimpleModule(ModuleDescriptor moduleDescriptor, ModuleOptions moduleOptions,
			ModuleDeploymentProperties deploymentProperties) {
		if (log.isInfoEnabled()) {
			log.info("creating simple module " + moduleDescriptor);
		}
		SimpleModuleDefinition definition = (SimpleModuleDefinition) moduleDescriptor.getModuleDefinition();
		ClassLoader moduleClassLoader = ModuleUtils.createModuleRuntimeClassLoader(definition, moduleOptions, this.parentClassLoader);

		Class<? extends SimpleModule> moduleClass = determineModuleClass((SimpleModuleDefinition) moduleDescriptor.getModuleDefinition(),
				moduleOptions);
		Assert.notNull(moduleClass,
				String.format("Required module artifacts are either missing or invalid. Unable to determine module type for module definition: '%s:%s'.",
						moduleDescriptor.getType(), moduleDescriptor.getModuleName()));
		return SimpleModuleCreator
				.createModule(moduleDescriptor, deploymentProperties, moduleClassLoader, moduleOptions, moduleClass);
	}

	private Class<? extends SimpleModule> determineModuleClass(SimpleModuleDefinition moduleDefinition,
			ModuleOptions moduleOptions) {
		String name = (String) moduleOptions.asPropertySource().getProperty(MODULE_EXECUTION_FRAMEWORK_KEY);
		if ("spark".equals(name)) {
			return NonBindingResourceConfiguredModule.class;
		}
		else if (ModuleUtils.resourceBasedConfigurationFile(moduleDefinition) != null) {
			return ResourceConfiguredModule.class;
		}
		else if (JavaConfiguredModule.basePackages(moduleDefinition).length > 0) {
			return JavaConfiguredModule.class;
		}
		return null;
	}

	/**
	 * Create a composite module based on the provided {@link ModuleDescriptor},
	 * {@link org.springframework.xd.module.options.ModuleOptions}, and
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param compositeDescriptor descriptor for the composed module
	 * @param options module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 * @return new composed module instance
	 * @see ModuleDescriptor#isComposed
	 */
	private Module createCompositeModule(ModuleDescriptor compositeDescriptor, ModuleOptions options,
			ModuleDeploymentProperties deploymentProperties) {
		List<ModuleDescriptor> children = compositeDescriptor.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");
		if (log.isInfoEnabled()) {
			log.info("creating composite module " + compositeDescriptor);
		}

		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDescriptor moduleDescriptor : children) {
			ModuleOptions moduleOptions = new PrefixNarrowingModuleOptions(options, moduleDescriptor.getModuleName());
			// due to parser results being reversed, we add each at index 0
			// todo: is it right to pass the composite deploymentProperties here?
			childrenModules
					.add(0, createAndConfigureModuleInstance(moduleDescriptor, moduleOptions, deploymentProperties));
		}
		return new CompositeModule(compositeDescriptor, deploymentProperties, childrenModules);
	}

	/**
	 * Takes a request and returns an instance of {@link ModuleOptions} bound with the request parameters. Binding is
	 * assumed to not fail, as it has already been validated on the admin side.
	 *
	 * @param descriptor module descriptor for which to bind request parameters
	 * @return module options bound with request parameters
	 */
	private ModuleOptions safeModuleOptionsInterpolate(ModuleDescriptor descriptor) {
		Map<String, String> parameters = descriptor.getParameters();
		ModuleOptionsMetadata moduleOptionsMetadata =
				moduleOptionsMetadataResolver.resolve(descriptor.getModuleDefinition());
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
		this.parentClassLoader = classLoader;
	}

	static class SimpleModuleCreator {

		public static <T extends SimpleModule> T createModule(ModuleDescriptor descriptor,
				ModuleDeploymentProperties deploymentProperties, ClassLoader classLoader, ModuleOptions moduleOptions,
				Class<T> requiredType) {
			Constructor<T> constructor = null;
			try {
				constructor = requiredType
						.getConstructor(ModuleDescriptor.class, ModuleDeploymentProperties.class, ClassLoader.class,
								ModuleOptions.class);
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			try {
				return constructor.newInstance(descriptor, deploymentProperties, classLoader, moduleOptions);
			}
			catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
