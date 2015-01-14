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
package org.springframework.xd.rxjava;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Test the RxJava based message handlers by using two types of {@link Processor}. The first is parameterized by
 * {@link org.springframework.messaging.Message} and the second by String to test extracting payload types and
 * wrapping return types in a Message.
 *
 * @author Mark Pollack
 */
public abstract class AbstractMessageHandlerTests {
    private final int numMessages = 10;

    @Autowired
    @Qualifier("outputChannel1")
    PollableChannel outputChannel1;

    @Autowired
    @Qualifier("outputChannel2")
    PollableChannel outputChannel2;

    @Autowired
    @Qualifier("toMessageHandlerChannel")
    MessageChannel toMessageHandlerChannel;

    @Autowired
    @Qualifier("toStringHandlerChannel")
    MessageChannel toStringHandlerChannel;

    @Test
    public void pojoBasedProcessor() throws IOException {
        sendPojoMessages();
        for (int i = 0; i < numMessages; i++) {
            Message<?> outputMessage = outputChannel1.receive(500);
            assertEquals("ping-pojopong", outputMessage.getPayload());
        }
    }

    @Test
    public void stringBasedProcessor() throws IOException {

        sendStringMessages();
        for (int i = 0; i < numMessages; i++) {
            Message<?> outputMessage = outputChannel2.receive(500);
            assertEquals("ping-stringpong", outputMessage.getPayload());
        }
    }

    private void sendPojoMessages() {
        Message<?> message = new GenericMessage<String>("ping");
        for (int i = 0; i < numMessages; i++) {
            toMessageHandlerChannel.send(message);
        }
    }

    private void sendStringMessages() {
        Message<?> message = new GenericMessage<String>("ping");
        for (int i = 0; i < numMessages; i++) {
            toStringHandlerChannel.send(message);
        }
    }
}
