/*
 * Copyright 2014 the original author or authors.
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


package org.springframework.xd.dirt.security;

import org.springframework.security.crypto.codec.Base64;

/**
 * Base class for Security Tests - allows for starting single node applications with different
 * configuration options - for testing different security scenarios.
 *
 * Supports the {@link org.springframework.xd.dirt.security.WithSpringConfigLocation} annotation that
 * allows a simple way to indicate what configuration file should the test be started with.
 *
 * @author Marius Bogoevici
 */
public abstract class AbstractSingleNodeApplicationSecurityTest {

	static String basicAuthorizationHeader(String username, String password) {
		return "Basic " + new String(Base64.encode((username + ":" + password).getBytes()));
	}


}
