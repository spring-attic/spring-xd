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

package org.springframework.xd.dirt.plugins;

import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.xd.dirt.container.decryptor.PropertiesDecryptor;
import org.springframework.xd.module.core.Module;

/**
 * Plugin to inject the {@link PropertiesDecryptor} into the module context if it exists.
 *
 * @author David Turanski
 * @since 1.3.1
 **/
public class PropertiesDecryptorPlugin extends AbstractPlugin {
	@Override
	public void preProcessModule(Module module) {
		if (getApplicationContext().containsBean(PropertiesDecryptor
				.DECRYPTOR_BEAN_NAME)){
			module.addListener(new PropertiesDecryptor(getApplicationContext().getBean
					(PropertiesDecryptor.DECRYPTOR_BEAN_NAME, TextEncryptor.class)));
		}
	}
	@Override public boolean supports(Module module) {
		return true;
	}
}
