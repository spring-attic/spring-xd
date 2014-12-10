/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.integration.bus;

/**
 * Rabbit message bus specific property names.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RabbitConnectionPropertyNames implements ConnectionPropertyNames {

	public static final String VHOST = "spring.rabbitmq.virtual_host";

	public static final String USERNAME = "spring.rabbitmq.username";

	public static final String PASSWORD = "spring.rabbitmq.password";

	public static final String ADDRESSES = "spring.rabbitmq.addresses";

	public static final String USE_SSL = "spring.rabbitmq.useSSL";

	public static final String SSL_PROPERTIES_LOCATION = "spring.rabbitmq.sslProperties";

	public String[] get() {
		return new String[] {
				VHOST, USERNAME, PASSWORD, ADDRESSES, USE_SSL, SSL_PROPERTIES_LOCATION
		};
	}

}
