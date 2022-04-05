/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spring.xd.ext.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * @author David Turanski
 **/
@Component
public class MyDecryptor implements TextEncryptor {
    private static Logger logger = LoggerFactory.getLogger(MyDecryptor.class);

	@Override public String encrypt(String text) {
		return null;
	}

	@Override public String decrypt(String encryptedText) {
		logger.warn("encrypted text {}", encryptedText);
		return encryptedText.toLowerCase();
	}
}
