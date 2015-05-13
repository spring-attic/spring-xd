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

package org.springframework.xd.integration.fixtures;

import org.springframework.util.Assert;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.*;


/**
 * A factory that provides fully instantiated job fixtures using default values.
 *
 * @author Glenn Renfro
 * @author David Turanski
 */
public class Jobs {

	private XdEnvironment environment;

	/**
	 * Initializes the instance with xdEnvironment
	 *
	 * @param environment
	 */
	public Jobs(XdEnvironment environment) {
		Assert.notNull(environment, "xdEnvironment must not be null");
		this.environment = environment;
	}

	/**
	 * Create an instance of the FileJdbc job with the default target dir, fileName, tableName and column names.
	 *
	 * @see FileJdbcJob for default values
	 *
	 * @return instance of a FileJDBCJob Fixture.
	 */
	public FileJdbcJob fileJdbcJob() {
		return FileJdbcJob.withDefaults();
	}

	/**
	 * Create an instance of the HdfsJdbc job with the default target dir, fileName, tableName and column names.
	 *
	 * @see HdfsJdbcJob for default values
	 *
	 * @return instance of a HdfsJDBCJob Fixture.
	 */
	public HdfsJdbcJob hdfsJdbcJob() {
		return HdfsJdbcJob.withDefaults();
	}

	/**
	 * Create an instance of the JdbcHdfs job with the default hdfs target dir, fileName and source sql statement.
	 *
	 * @see JdbcHdfsJob for default values
	 * @return instance of a JdbcHdfsJob Fixture.
	 */
	public JdbcHdfsJob jdbcHdfsJob() {
		return JdbcHdfsJob.withDefaults();
	}

	/**
	 * Create an instance of the JdbcHdfs job with the default hdfs target dir, fileName and source sql statement.
	 *
	 * @see JdbcHdfsJob for default values
	 * @return instance of a JdbcHdfsJob Fixture.
	 */
	public IncrementalJdbcHdfsJob incrementalJdbcHdfsJob() {
		return IncrementalJdbcHdfsJob.withDefaults();
	}

	/**
	 * Create an instance of the PartitionedJdbcHdfsJob job with the default hdfs target dir, fileName, table,
	 * columnNames, partitionColumn and partitions.
	 *
	 * @see PartitionedJdbcHdfsJob for default values
	 * @return instance of a PartitionedJdbcHdfsJob Fixture.
	 */
	public PartitionedJdbcHdfsJob partitionedJdbcHdfsJob() {
		return PartitionedJdbcHdfsJob.withDefaults();
	}

	/**
	 * Creates an instance of the FilePollHdfsJob using defaults.
	 *
	 * @param names A comma delimited list of column names
	 * @return an instance of the FilePollHdfsJob fixture.
	 */
	public FilePollHdfsJob filePollHdfsJob(String names) {
		return FilePollHdfsJob.withDefaults(names);
	}

	/**
	 * Create a new HdfsMongoDbJob that constructs a job that will read data from hdfs and output to mongo 
	 * using default values.
	 * Ensures that one can connect to MongoDB Server
	 *
	 * @return a HdfsMongoDbJob instance.
	 */
	public HdfsMongoDbJob hdfsMongoDb() {
		HdfsMongoDbJob hdfsMongoDbJob = HdfsMongoDbJob.withDefaults(environment.getMongoDbFactory());
		hdfsMongoDbJob.isReady();
		return hdfsMongoDbJob;
	}

	/**
	 * Create a FtpHdfsJob Fixture using defaults.
	 * @return a FtpHdfsJob instance
	 */
	public FtpHdfsJob ftpHdfsJob() {
		FtpHdfsJob ftpHdfsJob = FtpHdfsJob.withDefaults();
		return ftpHdfsJob;
	}

	/**
	 * Create a new SparkAppJob fixture instance that is configured via property files
	 * or environment variables.
	 * @return SparkAppJob instance
	 */
	public SparkAppJob sparkAppJob() {
		SparkAppJob sparkAppJob =  new SparkAppJob(environment.getSparkAppName(), environment.getSparkAppJar(),
				environment.getSparkAppMainClass(),environment.getSparkMaster());
		return sparkAppJob.sparkAppJarSource(environment.getSparkAppJarSource());
	}
}
