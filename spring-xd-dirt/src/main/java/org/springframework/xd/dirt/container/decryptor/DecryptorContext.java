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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Standalone application context to scan for a bean of type {@link TextEncryptor}
 *
 * @author David Turanski
 * @since 1.3.1
 **/
public class DecryptorContext {

	private static Logger logger = LoggerFactory.getLogger(DecryptorContext.class);

	private PropertiesDecryptor propertiesDecryptor;

	public DecryptorContext() {
		ApplicationContext decryptorContext = new SpringApplicationBuilder(
				DecryptorContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class)
				.headless(true)
				.web(false)
				.showBanner(false)
				.run();

		TextEncryptor textEncryptor = null;
		try {
			textEncryptor = decryptorContext.getBean(TextEncryptor.class);
			logger.debug("Registered TextEncryptor {}", textEncryptor.getClass()
					.getName());
		}

		catch (NoSuchBeanDefinitionException e) {
			if (e instanceof NoUniqueBeanDefinitionException) {
				throw e;
			}
		}
		/*
		 * A null textEncryptor effectively disables properties decryption
		 */
		propertiesDecryptor = new PropertiesDecryptor(textEncryptor);

	}

	public PropertiesDecryptor propertiesDecryptor() {
		return propertiesDecryptor;
	}


	@Configuration
	@ComponentScan(basePackages = {"spring.xd.ext.encryption"})
	static class DecryptorContextConfiguration {}
}
