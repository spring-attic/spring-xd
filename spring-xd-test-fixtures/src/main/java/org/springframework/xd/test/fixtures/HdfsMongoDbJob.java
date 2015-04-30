/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import java.util.Map;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;
import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;


/**
 * A test fixture that represents the HdfsMongoDb Job
 *
 * @author Glenn Renfro
 */
public class HdfsMongoDbJob extends AbstractModuleFixture<HdfsMongoDbJob> {

	public final static String DEFAULT_DIRECTORY = "/xd/hdfsmongodbtest";

	public final static String DEFAULT_FILE_NAME = "hdfsmongodbtest";

	public final static String DEFAULT_COLLECTION_NAME = "hdfsmongodbtest";

	public final static String DEFAULT_NAMES = "data";

	public final static String DEFAULT_ID_FIELD = "data";

	private String dir;

	private String fileName;

	private String collectionName;

	private String names;

	private String idField;

	private MongoTemplate mongoTemplate;

	private String host;

	private int port;

	/**
	 * Construct a new HdfsMongoDbJob using the provided directory and file names.
	 *
	 * @param dir the directory where the source file is located on hdfs
	 * @param fileName The file from which data will be pulled.
	 * @param collectionName the collection where the data will be written.
	 * @param names a comma delimited list of column names that are contained in the source file.
	 * @param mongoDbFactory The db factory for mongo
	 */
	public HdfsMongoDbJob(String dir, String fileName, String collectionName, String names, String idField,
			MongoDbFactory mongoDbFactory) {
		Assert.hasText(dir, "Dir must not be null or empty");
		Assert.hasText(fileName, "FileName must not be null or empty");
		Assert.hasText(collectionName, "CollectionName must not be null or empty");
		Assert.hasText(names, "Names must not be null nor empty");
		Assert.hasText(idField, "IdField must not be null nor empty");

		this.dir = dir;
		this.fileName = fileName;
		this.collectionName = collectionName;
		this.names = names;
		this.idField = idField;
		mongoTemplate = new MongoTemplate(mongoDbFactory);
		host = mongoTemplate.getDb().getMongo().getAddress().getHost();
		port = mongoTemplate.getDb().getMongo().getAddress().getPort();
	}

	/**
	 * Creates a HdfsMongoDb Instance using defaults.
	 * @param mongoDbFactory The mongoDbFactory that will be used to connect to mongo.
	 * @return HdfsMongoDb Instance
	 */
	public static HdfsMongoDbJob withDefaults(MongoDbFactory mongoDbFactory) {
		return new HdfsMongoDbJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, DEFAULT_COLLECTION_NAME, DEFAULT_NAMES,
				DEFAULT_ID_FIELD, mongoDbFactory);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		return String.format(
				"hdfsmongodb --resources=%s/%s --names=%s --idField=%s --collectionName=%s --host=%s --port=%s", dir,
				fileName, names, idField, collectionName, host, port);
	}

	/**
	 * Sets the directory where the file will be read from the hdfs.
	 * @param dir the directory path.
	 * @return The current instance of the HdfsMongoDbJob
	 */
	public HdfsMongoDbJob dir(String dir) {
		Assert.hasText(dir, "Dir should not be empty nor null");
		this.dir = dir;
		return this;
	}

	/**
	 * Sets the fileName of the file to be read
	 * @param fileName the name of the file
	 * @return The current instance of the HdfsMongoDbJob
	 */
	public HdfsMongoDbJob fileName(String fileName) {
		Assert.hasText(fileName, "FileName should not be empty nor null");
		this.fileName = fileName;
		return this;
	}

	/**
	 * Sets the name of the collection where the data will be written.
	 * @param collectionName The name of the collection
	 * @return The current instance of the HdfsMongoDbJob
	 */
	public HdfsMongoDbJob collectionName(String collectionName) {
		Assert.hasText(collectionName, "collectionName should not be empty nor null");
		this.collectionName = collectionName;
		return this;
	}

	/**
	 * Sets the column names that will be written to.
	 * @param names  Comma delimited list of column names.
	 * @return The current instance of the HdfsMongoDbJob
	 */
	public HdfsMongoDbJob names(String names) {
		Assert.hasText(names, "Names should not be empty nor null");
		this.names = names;
		return this;
	}

	/**
	 * Sets the idField for the data that will be inserted by this job
	 * @param idField The column that represents the id field;
	 * @return The current instance of HdfsMongoDbJob
	 */
	public HdfsMongoDbJob idField(String idField) {
		Assert.hasText(idField, "IdField should not be empty nor null");
		this.idField = idField;
		return this;
	}

	/**
	 * Sets the host where the mongo server is running
	 * @param host The mongo host server
	 * @return The current instance of HdfsMongoDbJob
	 */
	public HdfsMongoDbJob host(String host) {
		Assert.hasText(host, "Host can not be empty nor null");
		this.host = host;
		return this;
	}

	/**
	 * Sets the port that the HdfsMongoDb Job will transmit mongo commands
	 * @param port The port mongodb is monitoring
	 * @return the current instance of HdfsMongoDbJob
	 */
	public HdfsMongoDbJob port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Checks to see if the default mongo port 2000 is available on the configured host waiting up to 2 seconds
	 * 
	 * @throws IllegalStateException if can not connect in the specified timeout.
	 */
	public void isReady() {
		AvailableSocketPorts.ensureReady(this.getClass().getName(), host, port, 2000);
	}

	/**
	 * The drops the collection.  
	 * @param collectionName the name of the database to drop.
	 */
	public void dropCollection(String collectionName) {
		mongoTemplate.dropCollection(collectionName);
	}

	/**
	 * Returns a single object from the collection.
	 * 
	 * @param collectionName the name of the collection to query
	 * @return A Map with the content of the result
	 * @throws IllegalStateException if the number of objects in the collection is not one
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getSingleObject(String collectionName) {
		DBCursor cursor = mongoTemplate.getCollection(collectionName).find();
		if (cursor.count() != 1) {
			throw new IllegalStateException("Expected only one result but received " + cursor.count() + " entries");
		}
		DBObject dbObject = mongoTemplate.getCollection(collectionName).findOne();
		Map<String, String> result = null;
		if (dbObject != null) {
			result = dbObject.toMap();
		}
		return result;
	}

}
