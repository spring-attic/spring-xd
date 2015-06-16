/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.module.options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.xd.module.CompositeModuleDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.support.StringToEnumIgnoringCaseConverterFactory;

/**
 * The default implementation of {@link ModuleOptionsMetadataResolver} that deals with simple modules and reads the
 * companion {@code .properties} file sitting next to the module definition.
 *
 * <p>
 * The following strategies will be applied in turn:
 * <ul>
 * <li>look for a file named {@code <modulename>.properties} next to the module xml definition file</li>
 * <li>if that file exists
 * <ul>
 * <li>look for an {@value #OPTIONS_CLASS} property. If found, use a
 * {@link PojoModuleOptionsMetadata} backed by that POJO class name</li>
 * <li>use a {@link SimpleModuleOptionsMetadata} backed by keys of the form {@code options.<name>.description}.
 * Additionally, one can provide {@code options.<name>.default} and {@code options.<name>.type} properties.</li>
 * </ul>
 * <li>return an instance of {@link PassthruModuleOptionsMetadata}.
 * <ul>
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class DefaultModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver, ResourceLoaderAware {

	private static final Pattern DESCRIPTION_KEY_PATTERN = Pattern.compile("^options\\.([a-zA-Z\\-_0-9]+)\\.description$");

	/**
	 * Name of the property containing a POJO fully qualified classname, which will be used to create a
	 * {@link PojoModuleOptionsMetadata}.
	 */
	public static final String OPTIONS_CLASS = "options_class";

	private static final Map<String, Class<?>> SHORT_CLASSNAMES = new HashMap<String, Class<?>>();

	static {
		SHORT_CLASSNAMES.put("String", String.class);
		SHORT_CLASSNAMES.put("boolean", boolean.class);
		SHORT_CLASSNAMES.put("Boolean", Boolean.class);
		SHORT_CLASSNAMES.put("int", int.class);
		SHORT_CLASSNAMES.put("Integer", Integer.class);
		SHORT_CLASSNAMES.put("long", long.class);
		SHORT_CLASSNAMES.put("Long", Long.class);
		SHORT_CLASSNAMES.put("float", float.class);
		SHORT_CLASSNAMES.put("Float", Float.class);
		SHORT_CLASSNAMES.put("double", double.class);
		SHORT_CLASSNAMES.put("Double", Double.class);
	}

	private ConversionService conversionService;

	/**
	 * The resolver to delegate to when building metadata for a composite module.
	 */
	private ModuleOptionsMetadataResolver compositeResolver = this;


	private final DefaultModuleOptionsMetadataCollector defaultModuleOptionsMetadataCollector = new DefaultModuleOptionsMetadataCollector();

	private ResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();

	private boolean shouldCreateModuleClassLoader = true;

	/**
	 * Construct a new {@link DefaultModuleOptionsMetadataResolver}, using a
	 * {@link GenericConversionService} which is augmented with a {@link StringToEnumIgnoringCaseConverterFactory}.
	 * See also {@link PojoModuleOptionsMetadata}.
	 */
	public DefaultModuleOptionsMetadataResolver() {
		final GenericConversionService defaultConversionService = new GenericConversionService();
		defaultConversionService.addConverterFactory(new StringToEnumIgnoringCaseConverterFactory());
		this.conversionService = defaultConversionService;
	}

	public void setCompositeResolver(ModuleOptionsMetadataResolver compositeResolver) {
		this.compositeResolver = compositeResolver;
	}

	/**
	 * Set to true to create a new classloader (ParentLastURLClassLoader) when loading
	 * the module options metadata classes.
	 *
	 * @param shouldCreateModuleClassLoader
	 */
	public void setShouldCreateModuleClassLoader(boolean shouldCreateModuleClassLoader) {
		this.shouldCreateModuleClassLoader = shouldCreateModuleClassLoader;
	}

	private ModuleOptionsMetadata makeSimpleModuleOptions(Properties props) {
		SimpleModuleOptionsMetadata result = new SimpleModuleOptionsMetadata();
		for (Object key : props.keySet()) {
			if (key instanceof String) {
				String propName = (String) key;
				Matcher matcher = DESCRIPTION_KEY_PATTERN.matcher(propName);
				if (!matcher.matches()) {
					continue;
				}
				String optionName = matcher.group(1);
				String description = props.getProperty(propName);
				Object defaultValue = props.getProperty(String.format("options.%s.default", optionName));
				String type = props.getProperty(String.format("options.%s.type", optionName));
				Class<?> clazz = null;
				if (type != null) {
					String typeTrimmed = type.trim();
					if (SHORT_CLASSNAMES.containsKey(typeTrimmed)) {
						clazz = SHORT_CLASSNAMES.get(typeTrimmed);
					}
					else {
						try {
							clazz = Class.forName(typeTrimmed);
						}
						catch (ClassNotFoundException e) {
							throw new IllegalStateException("Can't find class used for type of option '"
									+ optionName
									+ "': " + typeTrimmed);
						}
					}
				}
				ModuleOption moduleOption = new ModuleOption(optionName, description).withDefaultValue(
						defaultValue).withType(clazz);
				result.add(moduleOption);
			}
		}
		return result;
	}

	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition definition) {
		if (!definition.isComposed()) {
			return resolveNormalMetadata((SimpleModuleDefinition) definition);
		}
		else {
			return resolveComposedModuleMetadata((CompositeModuleDefinition) definition);
		}

	}

	private ModuleOptionsMetadata resolveComposedModuleMetadata(CompositeModuleDefinition definition) {
		Map<String, ModuleOptionsMetadata> hierarchy = new HashMap<String, ModuleOptionsMetadata>();
		for (ModuleDefinition subModuleDefinition : definition.getChildren()) {
			ModuleOptionsMetadata subMetadata = compositeResolver.resolve(subModuleDefinition);
			// TODO: should be .getAlias() instead of name
			hierarchy.put(subModuleDefinition.getName(), subMetadata);
		}
		return new HierarchicalCompositeModuleOptionsMetadata(hierarchy);
	}

	private ModuleOptionsMetadata resolveNormalMetadata(SimpleModuleDefinition definition) {

		Resource moduleLocation = resourceLoader.getResource(definition.getLocation());
		Properties props = ModuleUtils.loadModuleProperties(definition);
		ClassLoader parentCL = ModuleOptionsMetadataResolver.class.getClassLoader();
		ClassLoader classLoaderToUse = (shouldCreateModuleClassLoader) ?
				ModuleUtils.createModuleDiscoveryClassLoader(moduleLocation, parentCL) : parentCL;
		if (props == null) {
			return inferModuleOptionsMetadata(definition, classLoaderToUse);
		}
		else {
			String pojoClass = props.getProperty(OPTIONS_CLASS);
			if (pojoClass != null) {
				List<ModuleOptionsMetadata> mixins = new ArrayList<ModuleOptionsMetadata>();
				createPojoOptionsMetadata(classLoaderToUse, pojoClass.trim(), mixins);
				return mixins.size() == 1 ? mixins.get(0) : new FlattenedCompositeModuleOptionsMetadata(mixins);
			}
			else {
				return makeSimpleModuleOptions(props);
			}
		}
	}

	/**
	 * Create {@link PojoModuleOptionsMetadata} out of the {@code pojoClass} and add them to the list.
	 * Also, recursively invoke itself with all classes mixed in {@code pojoClass} (that is,
	 * if {@code pojoClass} bears the {@link Mixin} annotation.
	 */
	private void createPojoOptionsMetadata(ClassLoader classLoaderToUse, String pojoClass,
			List<ModuleOptionsMetadata> mixins) {
		try {
			Class<?> clazz = Class.forName(pojoClass, true, classLoaderToUse);
			Mixin mixin = clazz.getAnnotation(Mixin.class);
			if (mixin != null) {
				for (Class<?> classToMixin : mixin.value()) {
					createPojoOptionsMetadata(classLoaderToUse, classToMixin.getName(), mixins);
				}
			}
			mixins.add(new PojoModuleOptionsMetadata(clazz, conversionService));
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Unable to load class used by ModuleOptionsMetadata: "
					+ pojoClass, e);
		}
	}

	/**
	 * Will parse the module xml definition file, looking for "${foo}" placeholders and advertise a {@link ModuleOption}
	 * for each of those.
	 *
	 * Note that this may end up in false positives and does not convey much information.
	 */
	private ModuleOptionsMetadata inferModuleOptionsMetadata(SimpleModuleDefinition definition,
			ClassLoader classLoaderToUse) {
		final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		Resource source = ModuleUtils.resourceBasedConfigurationFile(definition);
		if (source == null) {
			return new PassthruModuleOptionsMetadata();
		}

		AbstractBeanDefinitionReader reader = source.getFilename().endsWith("xml") ?
				new XmlBeanDefinitionReader(beanFactory) :
				new GroovyBeanDefinitionReader(beanFactory);

		reader.setResourceLoader(new PathMatchingResourcePatternResolver(classLoaderToUse));

		reader.loadBeanDefinitions(source);

		return defaultModuleOptionsMetadataCollector.collect(beanFactory);

	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = (ResourcePatternResolver) resourceLoader;
	}
}
