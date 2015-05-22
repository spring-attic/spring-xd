/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.distributed.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;
import org.springframework.xd.test.rabbit.RabbitTestSupport;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamPartitionTests extends AbstractDistributedTests {

	/**
	 * Enum for the various transport options used for partition testing.
	 */
	private enum Transport {
		rabbit, redis
	}

	@Rule
	public RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();


	/**
	 * Test data partitioning using Redis as the transport.
	 *
	 * @throws Exception
	 * @see #testPartitioning
	 */
	@Test
	public void testPartitioningWithRedis() throws Exception {
		testPartitioning(Transport.redis);
	}

	/**
	 * Test data partitioning using Rabbit as the transport.
	 *
	 * @throws Exception
	 * @see #testPartitioning
	 */
	@Test
	public void testPartitioningWithRabbit() throws Exception {
		testPartitioning(Transport.rabbit);
	}

	/**
	 * Test data partitioning for the given transport. This method
	 * asserts that data sent through a source is correctly routed
	 * to the expected sinks based on the partitioning configuration.
	 *
	 * @param transport transport to use for partition testing
	 * @throws Exception
	 */
	private void testPartitioning(Transport transport) throws Exception {
		Properties systemProperties = new Properties();
		systemProperties.setProperty("xd.transport", transport.toString());

		for (int i = 0; i < 2; i++) {
			startContainer(systemProperties);
		}

		SpringXDTemplate template = ensureTemplate();
		logger.info("Waiting for containers...");
		waitForContainers();
		logger.info("Containers running");

		String streamName = testName.getMethodName() + "-woodchuck";
		File file = File.createTempFile("temp", ".txt");
		file.deleteOnExit();
		int httpPort = SocketUtils.findAvailableServerSocket();
		template.streamOperations().createStream(
				streamName,
				String.format("http --port=%s | splitter --expression=payload.split(' ') | " +
						"file --dir=%s --name=${xd.container.id}", httpPort, file.getParent()), false);
		verifyStreamCreated(streamName);

		template.streamOperations().deploy(streamName, DeploymentPropertiesFormat.parseDeploymentProperties(
				"module.splitter.producer.partitionKeyExpression=payload,module.file.count=2"));

		// verify modules
		Map<String, Properties> modules = new HashMap<String, Properties>();
		int attempts = 0;
		while (attempts++ < 60) {
			Thread.sleep(500);
			for (ModuleMetadataResource module : template.runtimeOperations().listDeployedModules()) {
				modules.put(module.getContainerId() + ":" + module.getModuleType() + ":" + module.getName(),
						module.getDeploymentProperties());
			}
			if (modules.size() == 4) {
				break;
			}
		}
		assertEquals("timed out waiting for stream modules to deploy", 4, modules.size());

		RestTemplate restTemplate = new RestTemplate();
		String text = "how much wood would a woodchuck chuck if a woodchuck could chuck wood";
		int postAttempts = 0;
		while (postAttempts++ < 50) {
			Thread.sleep(100);
			try {
				ResponseEntity<?> entity = restTemplate.postForEntity("http://localhost:" + httpPort, text,
						String.class);
				assertEquals(HttpStatus.OK, entity.getStatusCode());
				break;
			}
			catch (Exception e) {
				// will try again
			}
		}

		File[] outputFiles = new File[2];
		for (Map.Entry<String, Properties> module : modules.entrySet()) {
			if (module.getKey().contains("sink")) {
				int index = Integer.parseInt(module.getValue().getProperty("consumer.partitionIndex"));
				String container = module.getKey().substring(0, module.getKey().indexOf(':'));
				File output = new File(file.getParent(), container + ".out");
				output.deleteOnExit();
				outputFiles[index] = output;
			}
		}

		String expectedPartition0 = "how\nchuck\nchuck\n";
		String expectedPartition1 = "much\nwood\nwould\na\nwoodchuck\nif\na\nwoodchuck\ncould\nwood\n";

		int fileAttempts = 0;
		String[] results = new String[2];
		while (fileAttempts++ < 10) {
			Thread.sleep(500);
			if (outputFiles[0].exists() && outputFiles[1].exists()) {
				results[0] = FileCopyUtils.copyToString(new FileReader(outputFiles[0]));
				results[1] = FileCopyUtils.copyToString(new FileReader(outputFiles[1]));
				if (results[0].length() == expectedPartition0.length()
						&& results[1].length() == expectedPartition1.length()) {
					break;
				}
			}
		}
		assertEquals(expectedPartition0, results[0]);
		assertEquals(expectedPartition1, results[1]);

	}

}
