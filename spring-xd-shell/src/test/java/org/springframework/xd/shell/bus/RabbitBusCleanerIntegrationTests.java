/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.shell.bus;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.dirt.integration.bus.BusUtils;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.test.rabbit.RabbitAdminTestSupport;
import org.springframework.xd.test.rabbit.RabbitTestSupport;


/**
 *
 * @author Gary Russell
 * @since 1.2
 */
public class RabbitBusCleanerIntegrationTests extends AbstractShellIntegrationTest {

	@Rule
	public RabbitAdminTestSupport adminTest = new RabbitAdminTestSupport();

	@Rule
	public RabbitTestSupport test = new RabbitTestSupport();

	@SuppressWarnings("unchecked")
	@Test
	public void testClean() throws Exception {
		RabbitAdmin admin = new RabbitAdmin(test.getResource());
		final String uuid = UUID.randomUUID().toString();
		String queueName = MessageBusSupport.applyPrefix("xdbus.",
				BusUtils.constructPipeName(uuid, 0));
		admin.declareQueue(new Queue(queueName));
		final FanoutExchange fanout = new FanoutExchange(
				MessageBusSupport.applyPrefix("xdbus.", MessageBusSupport.applyPubSub(
						BusUtils.constructTapPrefix(uuid) + ".foo.bar")));
		admin.declareExchange(fanout);
		RestTemplate template = new RestTemplate();
		URI uri = new URI("http://localhost:" + adminPort + "/streams/clean/rabbit/" + queueName.substring(6));
		RequestEntity<String> request = new RequestEntity<>(HttpMethod.DELETE, uri);
		HttpStatus status = HttpStatus.NO_CONTENT;
		ResponseEntity<?> reply = null;
		int n = 0;
		while (n++ < 100 && !status.equals(HttpStatus.OK)) {
			reply = template.exchange(request, Map.class);
			status = reply.getStatusCode();
			Thread.sleep(100);
		}
		assertEquals("Didn't get OK after 10 seconds", HttpStatus.OK, reply.getStatusCode());
		Map<String, List<String>> body = (Map<String, List<String>>) reply.getBody();
		assertEquals(2, body.size());
		List<String> queues = body.get("queues");
		assertEquals(queueName, queues.get(0));
		List<String> exchanges = body.get("exchanges");
		assertEquals(fanout.getName(), exchanges.get(0));
		reply = template.exchange(request, Map.class);
		assertEquals(HttpStatus.NO_CONTENT, reply.getStatusCode());
	}

}
