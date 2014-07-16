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

package org.springframework.xd.dirt.integration.rabbit;

import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.rabbitmq.client.ConnectionFactory;


/**
 * Factory bean to create a RabbitMQ ConnectionFactory, optionally enabling
 * SSL.
 *
 * @author Gary Russell
 */
// TODO: Move this to Spring AMQP.
public class RabbitConnectionFactoryBean extends AbstractFactoryBean<ConnectionFactory> {

	@Value("${spring.rabbitmq.useSSL}")
	private boolean useSSL;

	@Value("${spring.rabbitmq.sslProperties}")
	private Resource sslPropertiesLocation;

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void setSslPropertiesLocation(Resource sslPropertiesLocation) {
		this.sslPropertiesLocation = sslPropertiesLocation;
	}

	@Override
	public Class<?> getObjectType() {
		return ConnectionFactory.class;
	}

	@Override
	protected ConnectionFactory createInstance() throws Exception {
		ConnectionFactory rabbitConnectionFactory = new ConnectionFactory();
		if (this.useSSL) {
			setUpSSL(rabbitConnectionFactory);
		}
		return rabbitConnectionFactory;
	}

	private void setUpSSL(ConnectionFactory rabbitConnectionFactory) throws Exception {
		if (this.sslPropertiesLocation == null) {
			rabbitConnectionFactory.useSslProtocol();
		}
		else {
			Properties sslProperties = new Properties();
			sslProperties.load(this.sslPropertiesLocation.getInputStream());
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String keyStoreName = sslProperties.getProperty("keyStore");
			Assert.state(StringUtils.hasText(keyStoreName), "keyStore property required");
			String trustStoreName = sslProperties.getProperty("trustStore");
			Assert.state(StringUtils.hasText(trustStoreName), "trustStore property required");
			String keyStorePassword = sslProperties.getProperty("keyStore.passPhrase");
			Assert.state(StringUtils.hasText(keyStorePassword), "keyStore.passPhrase property required");
			String trustStorePassword = sslProperties.getProperty("trustStore.passPhrase");
			Assert.state(StringUtils.hasText(trustStorePassword), "trustStore.passPhrase property required");
			Resource keyStore = resolver.getResource(keyStoreName);
			Resource trustStore = resolver.getResource(trustStoreName);
			char[] keyPassphrase = keyStorePassword.toCharArray();
			char[] trustPassphrase = trustStorePassword.toCharArray();

			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(keyStore.getInputStream(), keyPassphrase);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keyPassphrase);

			KeyStore tks = KeyStore.getInstance("JKS");
			tks.load(trustStore.getInputStream(), trustPassphrase);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(tks);

			SSLContext context = SSLContext.getInstance("SSLv3");
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			rabbitConnectionFactory.useSslProtocol(context);
		}
	}

}
