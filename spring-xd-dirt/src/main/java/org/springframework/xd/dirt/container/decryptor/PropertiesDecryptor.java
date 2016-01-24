/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.container.decryptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.xd.dirt.container.initializer.OrderedContextInitializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decrypt properties from the environment and insert them with high priority so they
 * override the encrypted values. This class is injected as an ApplicationListener in
 * each application context in the XD hierarchy to be invoked before the context is
 * refreshed. This class is loosely based on spring-cloud-commons
 * org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer,
 * adopting the same conventions used to identify and process encrypted property values
 * in Spring Cloud applications.
 *
 * @author David Turanski
 * @since 1.3.1
 */
public class PropertiesDecryptor implements OrderedContextInitializer,
		ApplicationContextAware {
	private static Logger logger = LoggerFactory.getLogger(PropertiesDecryptor.class);

	public static final String DECRYPTED_PROPERTY_SOURCE_NAME = "decrypted";

	public static final String DECRYPTOR_BEAN_NAME = "propertiesDecryptor";

	private int order = Ordered.HIGHEST_PRECEDENCE + 15;

	private TextEncryptor decryptor;

	private boolean failOnError = true;

	private ApplicationContext applicationContext;

	/**
	 *
	 * @param decryptor the {@link TextEncryptor} used to decrypt properties.
	 * A null is ok here but properties will not be decrypted.
	 */
	public PropertiesDecryptor(TextEncryptor decryptor) {
		this.decryptor = decryptor;
	}

	/**
	 * Strategy to determine how to handle exceptions during decryption.
	 *
	 * @param failOnError the flag value (default true)
	 */
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		if (this.decryptor == null) {
			return;
		}

		ConfigurableApplicationContext applicationContext = event
				.getApplicationContext();
		if (!applicationContext.equals(this.applicationContext)){
			return;
		}

		/*
		 * register the decryptor as a bean so it is available to the
		 * PropertiesDecryptorPlugin later.
		 */
		if (!applicationContext.containsBean(DECRYPTOR_BEAN_NAME)) {
			applicationContext.getBeanFactory().registerSingleton
					(DECRYPTOR_BEAN_NAME, decryptor);
		}
		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		MutablePropertySources propertySources = environment.getPropertySources();

		Map<String, Object> map = decrypt(propertySources);
		if (!map.isEmpty()) {
			insert(propertySources,
					new MapPropertySource(DECRYPTED_PROPERTY_SOURCE_NAME, map));
		}
	}

	private void insert(MutablePropertySources propertySources,
			MapPropertySource propertySource) {
		propertySources.addFirst(propertySource);
	}

	public Map<String, Object> decrypt(PropertySources propertySources) {
		Map<String, Object> overrides = new LinkedHashMap<String, Object>();
		List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
		for (PropertySource<?> source : propertySources) {
			sources.add(0, source);
		}
		for (PropertySource<?> source : sources) {
			decrypt(source, overrides);
		}
		return overrides;
	}

	private Map<String, Object> decrypt(PropertySource<?> source) {
		Map<String, Object> overrides = new LinkedHashMap<String, Object>();
		decrypt(source, overrides);
		return overrides;
	}

	private void decrypt(PropertySource<?> source, Map<String, Object> overrides) {

		if (source instanceof EnumerablePropertySource) {

			EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
			for (String key : enumerable.getPropertyNames()) {
				Object property = source.getProperty(key);
				if (property != null) {
					String value = property.toString();

					if (value.startsWith("{cipher}")) {
						value = value.substring("{cipher}".length());
						try {
							value = this.decryptor.decrypt(value);
							logger.debug("Decrypted: key=" + key);
						} catch (Exception e) {
							String message = "Cannot decrypt: key=" + key;
							if (this.failOnError) {
								throw new IllegalStateException(message, e);
							}
							if (logger.isDebugEnabled()) {
								logger.warn(message, e);
							} else {
								logger.warn(message);
							}
							// Set value to empty to avoid making a password out of the
							// cipher text
							value = "";
						}
						overrides.put(key, value);
					}
				}
			}

		}
		else if (source instanceof CompositePropertySource) {

			for (PropertySource<?> nested : ((CompositePropertySource) source)
					.getPropertySources()) {
				decrypt(nested, overrides);
			}

		} else {
			logger.debug("ignored property source {} {}", source.getName(), source
					.getClass().getName());
		}
	}
}
