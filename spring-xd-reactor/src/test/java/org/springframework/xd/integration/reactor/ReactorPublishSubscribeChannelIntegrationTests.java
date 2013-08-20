package org.springframework.xd.integration.reactor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ReactorPublishSubscribeChannelIntegrationTests {

	@Autowired
	private SubscribableChannel output;
	@Autowired
	private TestMessageHandler  testHandler;

	@Test
	public void testReactorSubscribableChannel() throws InterruptedException {
		Message<?> msg = MessageBuilder.withPayload("Hello World!").build();
		output.send(msg);

		assertTrue("Latch did not time out", testHandler.getLatch().await(1, TimeUnit.SECONDS));
	}

}
