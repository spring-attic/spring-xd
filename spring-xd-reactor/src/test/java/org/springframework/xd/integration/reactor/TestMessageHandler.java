package org.springframework.xd.integration.reactor;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;

import java.util.concurrent.CountDownLatch;

/**
 * @author Jon Brisbin
 */
public class TestMessageHandler implements MessageHandler {

	final CountDownLatch latch = new CountDownLatch(1);

	public CountDownLatch getLatch() {
		return latch;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		latch.countDown();
	}

}
