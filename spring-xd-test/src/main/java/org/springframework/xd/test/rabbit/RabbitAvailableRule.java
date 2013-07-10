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

package org.springframework.xd.test.rabbit;


import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.xd.test.AbstractExternalServerAvailableRule;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RabbitAvailableRule extends AbstractExternalServerAvailableRule {

	@Override
	public Statement apply(Statement base, Description description) {
		CachingConnectionFactory connectionFactory = null;
		try {
			connectionFactory = new CachingConnectionFactory("localhost");
			Connection connection = connectionFactory.createConnection();
			connection.close();
		}
		catch (Exception e) {
			return super.failOrSkipTests("RABBIT", e);
		}
		finally {
			if (connectionFactory != null) {
				connectionFactory.destroy();
			}
		}
		return super.apply(base, description);
	}

}
