/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.xd.test;

import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestWatcher;
import org.junit.runners.model.Statement;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
public abstract class AbstractExternalServerAvailableRule extends TestWatcher {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final Statement failOrSkipTests(String server, Exception e) {
		String serversRequired = System.getenv("XD_EXTERNAL_SERVERS_REQUIRED");
		if (serversRequired != null && "true".equalsIgnoreCase(serversRequired)) {
			logger.error(server + " IS REQUIRED BUT NOT AVAILABLE", e);
			fail(server + " IS NOT AVAILABLE");
		}
		else {
			logger.error(server + " IS NOT AVAILABLE, SKIPPING TESTS", e);
		}
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {}
		};
	}

}
