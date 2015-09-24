/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.test.SingletonModuleRegistry;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainProducer;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainSupport;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.RandomConfigurationSupport;
import org.springframework.xd.test.domain.Book;
import reactor.fn.Supplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * @author Artem Bilan
 */
public class CassandraSinkTests {

	private static final String STREAM_NAME = "cassandraTest";

	private static final String MODULE_NAME = "cassandra";

	private static final String CASSANDRA_CONFIG = "spring-cassandra.yaml";

	private static final int PORT = 9043; // See spring-cassandra.yaml - native_transport_port


	private static Cluster cluster;

	private static CassandraOperations cassandraTemplate;

	private static SingleNodeApplication application;

	@BeforeClass
	public static void setUp() throws ConfigurationException, IOException, TTransportException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra(CASSANDRA_CONFIG, "build/embeddedCassandra");
		cluster = Cluster.builder()
				.addContactPoint("localhost")
				.withPort(PORT)
				.build();

		cluster.connect().execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s" +
				"  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };", STREAM_NAME));

		cassandraTemplate = new CassandraTemplate(cluster.connect(STREAM_NAME));

		new RandomConfigurationSupport();

		application = new SingleNodeApplication().run();
		SingleNodeIntegrationTestSupport integrationTest = new SingleNodeIntegrationTestSupport(application);
		integrationTest.addModuleRegistry(new SingletonModuleRegistry(ModuleType.sink, MODULE_NAME));
	}

	@AfterClass
	public static void cleanup() {
		if (cluster != null) {
			cluster.close();
		}
		EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
	}

	@Test
	public void testIngestQuery() throws Exception {
		String stream = String.format("%s --port=%s --initScript=%s --ingestQuery=\"%s\"",
				MODULE_NAME, PORT, "int-db.cql",
				"insert into book (isbn, title, author, pages, saleDate, inStock) values (?, ?, ?, ?, ?, ?)");

		SingleNodeProcessingChainProducer chain =
				SingleNodeProcessingChainSupport.chainProducer(application, STREAM_NAME, stream);

		List<Book> books = getBookList(5);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		Jackson2JsonObjectMapper mapper = new Jackson2JsonObjectMapper(objectMapper);


		chain.sendPayload(mapper.toJson(books));

		final Select select = QueryBuilder.select().all().from("book");

		assertEqualsEventually(5, new Supplier<Integer>() {

			@Override
			public Integer get() {
				return cassandraTemplate.select(select, Book.class).size();
			}

		});

		cassandraTemplate.truncate("book");
		chain.destroy();
	}


	private List<Book> getBookList(int numBooks) {

		List<Book> books = new ArrayList<>();

		Book b;
		for (int i = 0; i < numBooks; i++) {
			b = new Book();
			b.setIsbn(UUID.randomUUID());
			b.setTitle("Spring XD Guide");
			b.setAuthor("XD Guru");
			b.setPages(i * 10 + 5);
			b.setInStock(true);
			b.setSaleDate(new Date());
			books.add(b);
		}

		return books;
	}

	private static <T> void assertEqualsEventually(T expected, Supplier<T> actualSupplier) throws InterruptedException {
		int n = 0;
		while (!actualSupplier.get().equals(expected) && n++ < 100) {
			Thread.sleep(100);
		}
		assertTrue(n < 10);
	}

}
