/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;

import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

/**
 * @author Artem Bilan
 * @since 1.3.2
 */
public class MongoDbSourceModuleTests extends StreamTestSupport {

	private static final String STREAM = "mongoDbContents";

	private static final String DB = "test";

	private static final String COLLECTION = "testXdCollection";

	@BeforeClass
	public static void setup() {
		try {
			MongoClientOptions options = new MongoClientOptions.Builder()
					.connectTimeout(100)
					.build();

			Mongo mongo = new MongoClient(ServerAddress.defaultHost(), options);
			mongo.getDatabaseNames();
		}
		catch (Exception e) {
			throw new AssumptionViolatedException("Skipping test: MongoDb is not available");
		}
	}

	@Test
	public void testReadDataFromMongoDb() throws UnknownHostException {
		deployStream(STREAM,
				"mongodb --collectionName=" + COLLECTION + " --databaseName=" + DB + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertThat(message.getPayload(), instanceOf(DBObject.class));
				DBObject payload = (DBObject) message.getPayload();

				assertEquals("XD", payload.get("name"));
				assertEquals("SpringIO", payload.get("category"));
			}

		};

		StreamTestSupport.getSinkInputChannel(STREAM).subscribe(test);

		MongoTemplate mongoTemplate = new MongoTemplate(new SimpleMongoDbFactory(new MongoClient(), DB));

		mongoTemplate.save(JSON.parse("{'name' : 'XD', 'category' : 'SpringIO'}"), COLLECTION);

		test.waitForCompletion(10000);
		undeployStream(STREAM);
		assertTrue(test.getMessageHandled());
	}

}
