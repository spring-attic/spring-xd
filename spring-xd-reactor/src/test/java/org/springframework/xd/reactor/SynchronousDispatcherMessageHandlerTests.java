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
package org.springframework.xd.reactor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Test the {@link org.springframework.xd.reactor.SynchronousDispatcherMessageHandler} by using two types of
 * {@link org.springframework.xd.reactor.Processor}. The first is parameterized by
 * {@link org.springframework.messaging.Message} and the second by String to test extracting payload types and
 * wrapping return types in a Message.
 *
 * @author Mark Pollack
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/reactor.xml")
@DirtiesContext
public class SynchronousDispatcherMessageHandlerTests {

    private final int numMessages = 10;
    @Autowired
    PollableChannel outputChannel;
    @Autowired
    MessageChannel toHandlerChannel;

    @Test
    public void messageBasedProcessor() throws IOException {

        sendMessages();
        for (int i = 0; i < numMessages; i++) {
            Message<?> outputMessage = outputChannel.receive(500);
            assertEquals("ping-pong", outputMessage.getPayload());
        }
    }

    @Test
    public void pojoBasedProcessor() throws IOException {
        sendMessages();
        for (int i = 0; i < numMessages; i++) {
            Message<?> outputMessage = outputChannel.receive(500);
            assertEquals("ping-pong", outputMessage.getPayload());
        }
    }

    private void sendMessages() {
        Message<?> message = new GenericMessage<String>("ping");
        for (int i = 0; i < numMessages; i++) {
            toHandlerChannel.send(message);
        }
    }


}
