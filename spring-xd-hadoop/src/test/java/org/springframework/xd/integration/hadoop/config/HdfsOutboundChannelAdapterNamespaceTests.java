/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.integration.hadoop.config;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for the 'hdfs-outbound-channel-adapter' element.
 * 
 * @author Janne Valkealahti
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HdfsOutboundChannelAdapterNamespaceTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testWithId() {
		MessageChannel channel = context.getBean("hdfsOut", MessageChannel.class);
		assertNotNull(channel);
	}

	@Test
	public void testWithIdAndChannel() {
		EventDrivenConsumer adapter = context.getBean("adapter", EventDrivenConsumer.class);
		assertNotNull(adapter);
	}

}
