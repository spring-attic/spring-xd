/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.module.options;

import java.beans.PropertyDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.ValidationGroupsProvider;


/**
 * An implementation of {@link ModuleOptionsMetadata} that derives its information from a plain old java object:
 * <ul>
 * <li>public setters are reported as valid options</li>
 * <li>the type of the option is derived from the accepted type by the setter</li>
 * </ul>
 * 
 * {@link ModuleOptions} for such a POJO will work as follows:
 * <ul>
 * <li>an instance of the class will be created reflectively and injected with user provided values,</li>
 * <li>reported properties are computed from all the getters,</li>
 * <li>the POJO may bear JSR303 validation annotations, which will be used to validate the interpolated options,</li>
 * <li>if the POJO implements {@link ProfileNamesProvider}, profile names will be gathered from a reflective call to
 * {@link ProfileNamesProvider#profilesToActivate()}</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public class PojoModuleOptionsMetadata implements ModuleOptionsMetadata {

	private BeanWrapperImpl beanWrapper;

	private List<ModuleOption> options;

	/**
	 * Used to resolve the properties files that may be declared in @PropertySource annotations on the pojo class.
	 */
	private final ResourceLoader resourceLoader;

	/**
	 * Used to resolve placeholders in the locations of @PropertySource themselves. Not to be confused with the
	 * environment that is created to honor @Value annotations (although the latter may inherit from this one).
	 */
	private final Environment environment;

	/**
	 * Used to perform conversion from String representation of options to actual arguments of setters.
	 */
	private final ConversionService conversionService;

	public PojoModuleOptionsMetadata(Class<?> clazz) {
		this(clazz, null, null, null);
	}

	public PojoModuleOptionsMetadata(Class<?> clazz, ResourceLoader resourceLoader, Environment environment,
			ConversionService conversionService) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.conversionService = conversionService;

		Object bean = createAndInject(clazz);

		beanWrapper = new BeanWrapperImpl(bean);
		options = new ArrayList<ModuleOption>();
		for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
			String name = pd.getName();
			if (!beanWrapper.isWritableProperty(name)) {
				continue;
			}
			org.springframework.xd.module.options.spi.ModuleOption annotation = pd.getWriteMethod().getAnnotation(
					org.springframework.xd.module.options.spi.ModuleOption.class);
			Assert.notNull(
					annotation,
					String.format(
							"Setter method for option '%s' needs to bear the @%s annotation and provide a description",
							name,
							org.springframework.xd.module.options.spi.ModuleOption.class.getSimpleName()));

			String description = descriptionFromAnnotation(name, annotation);
			// Don't use pd.getPropertyType(), as it considers the getter first
			// which may be different from the setter type, which is what we semantically want here
			Class<?> type = BeanUtils.getWriteMethodParameter(pd).getParameterType();
			ModuleOption option = new ModuleOption(name, description).withType(type);
			if (beanWrapper.isReadableProperty(name)) {
				option.withDefaultValue(beanWrapper.getPropertyValue(name));
			}
			else {
				option.withDefaultValue(defaultFromAnnotation(annotation));
			}
			options.add(option);
		}
	}

	/**
	 * Returns a new instance of the {@code clazz} class, created and (possibly {@link Value} injected) by means of a
	 * throw away {@link ApplicationContext}.
	 */
	private Object createAndInject(Class<?> clazz) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(clazz);

		context.register(PropertySourcesPlaceholderConfigurer.class);
		MutablePropertySources propertySources = context.getEnvironment().getPropertySources();

		// process any @PropertySource annotations
		Set<PropertySource> annotations = AnnotationUtils.getRepeatableAnnotation(clazz, PropertySources.class,
				PropertySource.class);
		for (PropertySource annotation : annotations) {
			try {
				addPropertySourceToComposite(annotation, propertySources);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}


		context.refresh();

		Object bean = context.getBean(clazz);
		context.close();
		return bean;
	}

	private void addPropertySourceToComposite(PropertySource propertySource, MutablePropertySources propertySources)
			throws IOException {
		if (this.environment == null || this.resourceLoader == null) {
			throw new IllegalStateException(
					"Can't use @PropertySource with an instance that has not been fully configured");
		}
		String name = propertySource.name();
		String[] locations = propertySource.value();
		boolean ignoreResourceNotFound = propertySource.ignoreResourceNotFound();
		int locationCount = locations.length;
		if (locationCount == 0) {
			throw new IllegalArgumentException("At least one @PropertySource(value) location is required");
		}
		for (String location : locations) {
			Resource resource = this.resourceLoader.getResource(
					this.environment.resolveRequiredPlaceholders(location));
			try {
				if (!StringUtils.hasText(name) || propertySources.contains(name)) {
					// We need to ensure unique names when the property source will
					// ultimately end up in a composite
					ResourcePropertySource ps = StringUtils.hasText(name) ? new ResourcePropertySource(name, resource)
							: new ResourcePropertySource(resource);
					propertySources.addLast(ps);
				}
				else {
					propertySources.addLast(new ResourcePropertySource(name, resource));
				}
			}
			catch (FileNotFoundException ex) {
				if (!ignoreResourceNotFound) {
					throw ex;
				}
			}
		}
	}

	private Object defaultFromAnnotation(org.springframework.xd.module.options.spi.ModuleOption annotation) {
		String value = annotation.defaultValue();
		return org.springframework.xd.module.options.spi.ModuleOption.NO_DEFAULT.equals(value) ? null : value;
	}

	/**
	 * Read the description from the value() attribute of the annotation on the getter.
	 */
	private String descriptionFromAnnotation(String optionName,
			org.springframework.xd.module.options.spi.ModuleOption annotation) {
		Assert.hasLength(
				annotation.value(),
				String.format(
						"Setter method for option '%s' needs to bear the @%s annotation and provide a non-empty description",
						optionName,
						org.springframework.xd.module.options.spi.ModuleOption.class.getSimpleName()));
		return annotation.value();
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.iterator();
	}

	@Override
	public ModuleOptions interpolate(Map<String, String> raw) throws BindException {
		bindAndValidate(raw);

		return new ModuleOptions() {

			@Override
			public EnumerablePropertySource<?> asPropertySource() {
				return new EnumerablePropertySource<BeanWrapper>(this.toString(), beanWrapper) {

					@Override
					public String[] getPropertyNames() {
						List<String> result = new ArrayList<String>();
						for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
							String name = pd.getName();
							if (beanWrapper.isReadableProperty(name) && !"class".equals(name)) {
								result.add(name);
							}
						}
						return result.toArray(new String[result.size()]);
					}

					@Override
					public Object getProperty(String name) {
						if (Arrays.asList(getPropertyNames()).contains(name)) {
							return beanWrapper.getPropertyValue(name);
						}
						else {
							return null;
						}
					}
				};
			}

			@Override
			public String[] profilesToActivate() {
				if (beanWrapper.getWrappedInstance() instanceof ProfileNamesProvider) {
					return ((ProfileNamesProvider) beanWrapper.getWrappedInstance()).profilesToActivate();
				}
				else {
					return super.profilesToActivate();
				}
			}
		};
	}

	@SuppressWarnings("unchecked")
	private void bindAndValidate(Map<String, String> raw) throws BindException {
		DataBinder dataBinder = new DataBinder(beanWrapper.getWrappedInstance());
		dataBinder.setIgnoreUnknownFields(false);
		dataBinder.setConversionService(conversionService);
		MutablePropertySources mps = new MutablePropertySources();
		mps.addFirst(new MapPropertySource("options", (Map) raw));
		try {
			dataBinder.bind(new PropertySourcesPropertyValues(mps));
		}
		catch (InvalidPropertyException e) {
			dataBinder.getBindingResult().addError(new FieldError("options", e.getPropertyName(), e.getMessage()));
		}

		CustomValidatorBean validator = new CustomValidatorBean();
		validator.afterPropertiesSet();
		dataBinder.setValidator(validator);

		Class<?>[] groups = determineGroupsToUse(beanWrapper.getWrappedInstance());
		dataBinder.validate((Object[]) groups);

		if (dataBinder.getBindingResult().hasErrors()) {
			throw new BindException(dataBinder.getBindingResult());
		}
	}

	private Class<?>[] determineGroupsToUse(Object pojo) {
		if (pojo instanceof ValidationGroupsProvider) {
			ValidationGroupsProvider groupsProvider = (ValidationGroupsProvider) pojo;
			return groupsProvider.groupsToValidate();
		}
		else {
			return ValidationGroupsProvider.DEFAULT_GROUP;
		}
	}

	@Override
	public String toString() {
		return String.format("%s backed by %s, defining options [%s]", getClass().getSimpleName(),
				beanWrapper.getWrappedClass(), options);
	}

}
