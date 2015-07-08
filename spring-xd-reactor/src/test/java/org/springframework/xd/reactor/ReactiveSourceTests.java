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
package org.springframework.xd.reactor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.reactor.source.FooSource;

import static org.junit.Assert.assertEquals;

/**
 * Test the {@link ReactorMessageHandler} by using Message types of
 * {@link ReactiveProcessor}.
 *
 * @author Mark Pollack
 * @author Stepane Maldini
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("reactor.xml")
@DirtiesContext
@ActiveProfiles("foo-source")
public class ReactiveSourceTests {

	@Autowired
	FooSource fooSource;

	@Test
	public void pojoBasedProcessor() throws Exception {
		for (int i = 0; i < 20; i++) {
			Message<?> outputMessage = fooSource.outputSource().receive(2000);
			assertEquals("" + i, outputMessage.getPayload().toString());
		}
	}
}
