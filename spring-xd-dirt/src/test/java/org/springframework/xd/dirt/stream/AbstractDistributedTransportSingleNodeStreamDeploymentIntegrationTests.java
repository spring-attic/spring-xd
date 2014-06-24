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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.xd.dirt.integration.bus.Binding;

/**
 * Base class for testing non-local transports, such as RabbitMQ and Redis.
 *
 * @author Mark Fisher
 */
public abstract class AbstractDistributedTransportSingleNodeStreamDeploymentIntegrationTests extends
		AbstractSingleNodeStreamDeploymentIntegrationTests {

	@Test
	public void shortCircuitingEnabledWithExplicitModuleCounts() throws InterruptedException {
		String streamName = "shortCircuitingEnabledWithExplicitModuleCounts";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.http.count", "0");
		props.put("module.log.count", "0");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		System.out.println(bindings);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("direct", producerBinding.getType());
	}

	@Test
	public void shortCircuitingEnabledWithWildcardModuleCount() throws InterruptedException {
		String streamName = "shortCircuitingEnabledWithWildcardModuleCount";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		integrationSupport.deployStream(sd, Collections.singletonMap("module.*.count", "0"));
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("direct", producerBinding.getType());
	}

	@Test
	public void shortCircuitingEnabledWithCriteria() throws InterruptedException {
		String streamName = "shortCircuitingEnabledWithCriteria";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.http.count", "0");
		props.put("module.http.criteria", "true");
		props.put("module.log.count", "0");
		props.put("module.log.criteria", "true");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("direct", producerBinding.getType());
	}

	@Test
	public void shortCircuitingNotEnabledWithMismatchedCounts() throws InterruptedException {
		String streamName = "shortCircuitingNotEnabledWithMismatchedCounts";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.http.count", "0");
		props.put("module.log.count", "3");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("producer", producerBinding.getType());
	}

	@Test
	public void shortCircuitingNotEnabledWithOverriddenCount() throws InterruptedException {
		String streamName = "shortCircuitingNotEnabledWithOverriddenCount";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.*.count", "0");
		props.put("module.log.count", "3");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("producer", producerBinding.getType());
	}

	@Test
	public void shortCircuitingNotEnabledWithMismatchedCriteria() throws InterruptedException {
		String streamName = "shortCircuitingNotEnabledWithMismatchedCriteria";
		StreamDefinition sd = new StreamDefinition(streamName, "http | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.http.count", "0");
		props.put("module.http.criteria", "true");
		props.put("module.log.count", "0");
		props.put("module.log.criteria", "!false");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		assertEquals(2, bindings.size());
		Binding consumerBinding = bindings.get(0);
		Binding producerBinding = bindings.get(1);
		assertEquals("consumer", consumerBinding.getType());
		assertEquals("producer", producerBinding.getType());
	}

	@Test
	public void shortCircuitingEnabledForPartOfStream() throws InterruptedException {
		String streamName = "shortCircuitingEnabledForPartOfStream";
		StreamDefinition sd = new StreamDefinition(streamName, "http | filter | log");
		integrationSupport.streamDefinitionRepository().save(sd);
		Map<String, String> props = new HashMap<String, String>();
		props.put("module.http.count", "0");
		props.put("module.filter.count", "0");
		props.put("module.log.count", "1");
		integrationSupport.deployStream(sd, props);
		List<Binding> bindings = getMessageBusBindingsForStream(streamName);
		System.out.println(bindings);
		assertEquals(4, bindings.size());
		Binding logConsumerBinding = bindings.get(0);
		Binding filterConsumerBinding = bindings.get(1);
		Binding filterProducerBinding = bindings.get(2);
		Binding httpProducerBinding = bindings.get(3);
		assertEquals("inbound." + streamName + ".1", logConsumerBinding.getEndpoint().getComponentName());
		assertEquals("consumer", logConsumerBinding.getType());
		assertEquals("inbound." + streamName + ".0", filterConsumerBinding.getEndpoint().getComponentName());
		assertEquals("consumer", filterConsumerBinding.getType());
		assertEquals("outbound." + streamName + ".1", filterProducerBinding.getEndpoint().getComponentName());
		assertEquals("producer", filterProducerBinding.getType());
		assertEquals("outbound." + streamName + ".0", httpProducerBinding.getEndpoint().getComponentName());
		assertEquals("direct", httpProducerBinding.getType());
	}

}
