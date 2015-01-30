/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Driver;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.mongodb.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

/**
 * Extracts the host and port information for the XD Instances.
 * Assumes that the host that runs the RabbitMQ broker is the same host that runs the admin server.
 *
 * @author Glenn Renfro
 */

public class XdEnvironment implements BeanClassLoaderAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(XdEnvironment.class);

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	// Environment Keys
	public static final String XD_ADMIN_HOST = "xd_admin_host";

	public static final String XD_HTTP_PORT = "xd_http_port";

	public static final String XD_JMX_PORT = "xd_jmx_port";

	// Result Environment Variables
	public static final String RESULT_LOCATION = "/tmp/xd/output";

	public static final String HTTP_PREFIX = "http://";


	@Value("${xd_admin_host:}")
	private String adminHost;

	private URL adminServerUrl;

	@Value("${xd_jmx_port}")
	private int jmxPort;

	@Value("${xd_container_log_dir}")
	private String containerLogLocation;

	@Value("${xd_base_dir}")
	private String baseXdDir;

	@Value("${xd_http_port}")
	private int httpPort;

	@Value("${xd_pvt_keyfile:}")
	private String privateKeyFileName;

	private String privateKey;

	@Value("${xd_run_on_ec2:true}")
	private boolean isOnEc2;

	// JDBC Attributes
	@Value("${jdbc_url}")
	private String jdbcUrl;

	@Value("${jdbc_username}")
	private String jdbcUsername;

	@Value("${jdbc_password}")
	private String jdbcPassword;

	@Value("${jdbc_database}")
	private String jdbcDatabase;

	@Value("${jdbc_driver}")
	private String jdbcDriver;

	// JMS Attributes
	@Value("${jms_host}")
	private String jmsHost;

	@Value("${jms_port}")
	private int jmsPort;

	// Twitter Search Attributes
	// Twitter Keys
	@Value("${twitterConsumerKey}")
	private String twitterConsumerKey;

	@Value("${twitterConsumerSecretKey}")
	private String twitterConsumerSecretKey;

	@Value("${twitterAccessToken}")
	private String twitterAccessToken;

	@Value("${twitterAccessTokenSecret}")
	private String twitterAccessTokenSecret;

	@Value("${spring.mongo.host}")
	private String mongoHost;

	@Value("${spring.mongo.port}")
	private String mongoPort;

	private SimpleDriverDataSource dataSource;

	MongoDbFactory mongoDbFactory;

	private CachingConnectionFactory rabbitConnectionFactory;

	@Value("${spring.hadoop.fsUri}")
	private String nameNode;

	@Value("${dfs.datanode.http.port:50075}")
	private String dataNodePort;

	@Value("${gemfire.host}")
	private String gemfireHost;

	@Value("${gemfire.port}")
	private int gemfirePort;

	@Value("${kafka.zkConnect}")
	private String zkConnect;

	@Value("${kafka.brokerList}")
	private String kafkaBrokerList;

	@Value("${tcp.client.host}")
	private String tcpClientHost;

	@Value("${tcp.client.port}")
	private int tcpClientPort;

	@Value("${spark.app.name}")
	private String sparkAppName;

	@Value("${spark.app.jar}")
	private String sparkAppJar;

	@Value("${spark.app.jar.source}")
	private String sparkAppJarSource;

	@Value("${spark.master}")
	private String sparkMaster;

	@Value("${spark.app.main.class}")
	private String sparkAppMainClass;

	private Properties artifactProperties;

	/**
	 * If not running tests on a local XD Instance it will retrieve the information from the artifact and setup the
	 * environment to test against a remote XD. Als initializes the dataSource for JDBC Tests.
	 *
	 * @throws MalformedURLException
	 */
	@PostConstruct()
	public void initalizeEc2Environment() throws MalformedURLException {
		if (isOnEc2()) {
			artifactProperties = ConfigUtil.getPropertiesFromArtifact();
			adminServerUrl = new URL(artifactProperties.getProperty(XD_ADMIN_HOST));
			jmxPort = Integer.parseInt(artifactProperties.getProperty(XD_JMX_PORT));
			httpPort = Integer.parseInt(artifactProperties.getProperty(XD_HTTP_PORT));
		}
		else {
			adminServerUrl = new URL(adminHost);
		}

		if (jdbcUrl == null) {
			return;
		}
		dataSource = new SimpleDriverDataSource();
		rabbitConnectionFactory = new CachingConnectionFactory(adminServerUrl.getHost());
		try {
			@SuppressWarnings("unchecked") Class<? extends Driver> classz =
					(Class<? extends Driver>) ClassUtils.forName(jdbcDriver, beanClassLoader);
			dataSource.setDriverClass(classz);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("failed to load class: " + jdbcDriver, e);
		}
		String resolvedJdbcUrl = String.format(jdbcUrl, jdbcDatabase);
		dataSource.setUrl(resolvedJdbcUrl);
		if (jdbcUsername != null) {
			dataSource.setUsername(jdbcUsername);
		}
		if (jdbcPassword != null) {
			dataSource.setPassword(jdbcPassword);
		}

		try {
			mongoDbFactory = new SimpleMongoDbFactory(new MongoClient(mongoHost + ":" + mongoPort), "xd");
		}
		catch (UnknownHostException unknownHostException) {
			throw new IllegalStateException(unknownHostException.getMessage(), unknownHostException);
		}

	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	public URL getAdminServerUrl() {
		return adminServerUrl;
	}

	public int getJmxPort() {
		return jmxPort;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public String getPrivateKey() {
		// Initialize private key if not already set.
		if (isOnEc2 && privateKey == null) {
			privateKey = ConfigUtil.getPrivateKey(privateKeyFileName);
		}
		return privateKey;
	}

	public boolean isOnEc2() {
		return isOnEc2;
	}

	public String getContainerLogLocation() {
		return containerLogLocation;
	}

	public String getBaseDir() {
		return baseXdDir;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public CachingConnectionFactory getRabbitConnectionFactory() {
		return rabbitConnectionFactory;
	}

	public String getJmsHost() {
		return jmsHost;
	}


	public int getJmsPort() {
		return jmsPort;
	}

	/**
	 * Default value is the same as the admin server host.
	 *
	 * @return the host where the rabbitmq broker is running.
	 */
	public String getRabbitMQHost() {
		return getAdminServerUrl().getHost();
	}

	/**
	 * The twitter consumer key
	 *
	 * @return consumer key
	 */
	public String getTwitterConsumerKey() {
		return twitterConsumerKey;
	}

	/**
	 * The twitter consumer secret key
	 *
	 * @return consumer secret key
	 */
	public String getTwitterConsumerSecretKey() {
		return twitterConsumerSecretKey;
	}

	/**
	 * The Twitter Access Token
	 *
	 * @return twitter access token
	 */
	public String getTwitterAccessToken() {
		return twitterAccessToken;
	}

	/**
	 * The Twitter Access Token Secret
	 *
	 * @return twitter access token secret
	 */
	public String getTwitterAccessTokenSecret() {
		return twitterAccessTokenSecret;
	}

	/**
	 * The hadoop name node that is available for this environment.
	 *
	 * @return the hadoop name node.
	 */
	public String getNameNode() {
		return nameNode;
	}

	/**
	 * The hadoop data node port that is available for this environment.
	 *
	 * @return the data node port.
	 */
	public String getDataNodePort() {
		return dataNodePort;
	}

	/**
	 * The mongo db factory to be used for the tests.
	 *
	 * @return The current mongoDbFactory instance.
	 */
	public MongoDbFactory getMongoDbFactory() {
		return mongoDbFactory;
	}

	/**
	 * Retrieves the host where the mongo server is running.
	 *
	 * @return the mongo host
	 */
	public String getMongoHost() {
		return mongoHost;
	}

	/**
	 * Retrieves the port that the mongo server is monitoring.
	 *
	 * @return the mongo server port.
	 */
	public String getMongoPort() {
		return mongoPort;
	}

	/**
	 * Retrieves the Gemfire host.
	 *
	 * @return the host
	 */
	public String getGemfireHost() {
		return gemfireHost;
	}

	/**
	 * Retrieves the Gemfire port.
	 *
	 * @return the port
	 */
	public int getGemfirePort() {
		return gemfirePort;
	}

	/**
	 * Retrieves the Kafka Broker List.
	 *
	 * @return the kafka broker list
	 */
	public String  getKafkaBrokerList() {
		return kafkaBrokerList;
	}

	/**
	 * Retrieves the Kafka ZK Connection String.
	 *
	 * @return the kafka ZK Connection String
	 */
	public String  getKafkaZkConnect() {
		return zkConnect;
	}

	/**
	 * Retrieves the TCP Client port.
	 *
	 * @return the port
	 */
	public int getTcpClientPort() {
		return tcpClientPort;
	}

	/**
	 * Retrieves the TCP Client host.
	 *
	 * @return the host
	 */
	public String getTcpClientHost() {
		return tcpClientHost;
	}

	/**
	 * Retrieves the name associated with the Spark App
	 * @return string containing the spark app name
	 */
	public String getSparkAppName() {
		return sparkAppName;
	}

	/**
	 * Retrieves the location of the SparkApp Jar
	 * @return the location of the spark app jar.
	 */
	public String getSparkAppJar() {
		return sparkAppJar;
	}

	/**
	 * Retrieves URI (spark://host:port or the local setting (local[x]) for the spark master
	 * @return the spark master to be used by the test.
	 */
	public String getSparkMaster() {
		return sparkMaster;
	}

	/**
	 * Retrieves the package.class of the Spark App
	 * @return package and class of the spark app
	 */
	public String getSparkAppMainClass() {
		return sparkAppMainClass;
	}

	/**
	 * Retrieves the location of the spark app jar to be used for the test.
	 * @return The location of the spark app jar to be used for the test.
	 */
	public String getSparkAppJarSource() {
		return sparkAppJarSource;
	}
}
