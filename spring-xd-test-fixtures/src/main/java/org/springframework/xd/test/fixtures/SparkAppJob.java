/*
 *
 *  * Copyright 2014-2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * A test fixture that represents the Spark App Job
 *
 * @author Glenn Renfro
 */
public class SparkAppJob extends AbstractModuleFixture<SparkAppJob> {

	public final static String DEFAULT_NAME = "EC2TEST3SPARKNAME";

	public final static String DEFAULT_APP_JAR = "/home/ubuntu/application-test-sparkapp-1.1.0.BUILD-SNAPSHOT.jar";

	public final static String DEFAULT_MASTER = "localhost[1]";

	public final static String DEFAULT_MAIN_CLASS = "spark.SparkPi";

	public final static String DEFAULT_APP_SOURCE_JAR = "/tmp/application-test-sparkapp-1.1.0.BUILD-SNAPSHOT.jar";


	private String sparkAppName;

	private String sparkAppJar;

	private String sparkAppMainClass;

	private String sparkMaster;

	private String sparkAppJarSource;

	/**
	 * Construct a new SparkAppJob fixture using the provided spark app's name, jar,
	 * main class and master.
	 * @param sparkAppName the name of the spark app
	 * @param sparkAppJar the jar that contains the spark app
	 * @param sparkAppMainClass the package and class name
	 * @param sparkMaster either local[x] or the spark://URL
	 */
	public SparkAppJob(String sparkAppName, String sparkAppJar, String sparkAppMainClass, String sparkMaster) {
		Assert.hasText(sparkAppName, "sparkAppName must not be null or empty");
		Assert.hasText(sparkAppJar, "sparkAppJar must not be null or empty");
		Assert.hasText(sparkAppMainClass, "sparkAppMainClass must not be null nor empty");
		Assert.hasText(sparkMaster, "sparkMaster must not be null nor empty");

		this.sparkAppName = sparkAppName;
		this.sparkAppJar = sparkAppJar;
		this.sparkAppMainClass = sparkAppMainClass;
		this.sparkMaster = sparkMaster;
		sparkAppJarSource = DEFAULT_APP_SOURCE_JAR;
	}

	/**
	 * Creates an instance of the SparkAppJob fixture using defaults.
	 *
	 * @return an instance of the SparkAppJob fixture.
	 */
	public static SparkAppJob withDefaults() {
		return new SparkAppJob(DEFAULT_NAME, DEFAULT_APP_JAR, DEFAULT_MAIN_CLASS, DEFAULT_MASTER);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		return String.format(
				"sparkapp --appJar=%s --name=%s --master=%s  --mainClass=%s",
				sparkAppJar, sparkAppName, sparkMaster, sparkAppMainClass);
	}

	/**
	 * Construct  the URI location of the jar
	 * @param jarLocation String location of the jar
	 * @return the URI for the location of the JAR
	 */
	public URI getJarURI(String jarLocation){
			URI result;
			try {
				result = new URI("file://" + jarLocation);
			}
			catch (URISyntaxException e) {
				throw new IllegalStateException("jar location is not properly formatted", e);
			}
			return result;
	}

	/**
	 * Sets the spark App Name for the fixture
	 * @param sparkAppName the name for the spark app
	 * @return current instance the job.
	 */
	public SparkAppJob sparkAppName(String sparkAppName){
		Assert.hasText(sparkAppName, "sparkAppName must not be null or empty");
		this.sparkAppName = sparkAppName;
		return this;
	}

	/**
	 * Sets the location of the spark app jar
	 * @param sparkAppJar the location of the spark app jar
	 * @return current instance the job.
	 */
	public SparkAppJob sparkAppJar(String sparkAppJar){
		Assert.hasText(sparkAppJar, "sparkAppJar must not be null or empty");
		this.sparkAppJar = sparkAppJar;
		return this;
	}

	/**
	 * Sets the package and class of spark app
	 * @param sparkAppMainClass the package and class of the spark app
	 * @return current instance the job.
	 */
	public SparkAppJob sparkAppMainClass(String sparkAppMainClass){
		Assert.hasText(sparkAppMainClass, "sparkAppMainClass must not be null nor empty");
		this.sparkAppMainClass = sparkAppMainClass;
		return this;
	}

	/**
	 * Sets The url of the spark master(spark://url:7077 or localhost[x]
	 * @param sparkMaster the location of the spark master or localhost
	 * @return current instance the job.
	 */
	public SparkAppJob sparkMaster(String sparkMaster){
		Assert.hasText(sparkMaster, "sparkMaster must not be null nor empty");
		this.sparkMaster = sparkMaster;
		return this;
	}
	/**
	 * Sets the location of a source jar that can be copied to the target location where
	 * XD can access the jar
	 * @param sparkAppJarSource the location of the spark app jar.
	 * @return current instance the job.
	 */
	public SparkAppJob sparkAppJarSource(String sparkAppJarSource){
		Assert.hasText(sparkAppJarSource, "sparkAppJarSource must not be null nor empty");
		this.sparkAppJarSource = sparkAppJarSource;
		return this;
	}

	public String getSparkAppName() {
		return sparkAppName;
	}

	public String getSparkAppJar() {
		return sparkAppJar;
	}

	public String getSparkAppMainClass() {
		return sparkAppMainClass;
	}

	public String getSparkMaster() {
		return sparkMaster;
	}

	public String getSparkAppJarSource(){
		return sparkAppJarSource;
	}
}
