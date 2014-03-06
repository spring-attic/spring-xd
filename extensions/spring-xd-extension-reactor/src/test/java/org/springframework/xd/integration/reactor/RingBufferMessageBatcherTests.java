package org.springframework.xd.integration.reactor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RingBufferMessageBatcherTests {

	static long TIMEOUT   = 1000;
	static int  MSG_COUNT = 5000;

	@Autowired
	RingBufferMessageBatcher batcher;
	@Autowired
	PublishSubscribeChannel  output;

	AtomicLong counter;

	@Before
	public void setup() {
		counter = new AtomicLong(0);
	}

	@Test
	public void ringBufferMessageBatcherReleasesWhenFull() throws InterruptedException {
		MessageHandler batchHandler = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				if(message.getPayload() instanceof List) {
					counter.incrementAndGet();
				}
			}
		};
		output.subscribe(batchHandler);

		for(int i = 0; i < MSG_COUNT; i++) {
			batcher.handleMessage(new GenericMessage<String>("i=" + i));
		}

		assertThat("Latch was counted down", counter.get(), is(4l));
	}

	@Test
	public void testBatchMessageHandlerThroughput() {
		MessageHandler batchHandler = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				counter.addAndGet(((List)message.getPayload()).size());
			}
		};
		output.subscribe(batchHandler);
		Message<?> msg = new GenericMessage<Object>("Hello World!");

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < TIMEOUT) {
			batcher.handleMessage(msg);
		}
		long end = System.currentTimeMillis();
		double elapsed = end - start;
		int throughput = (int)(counter.get() / (elapsed / 1000));

		System.out.format("batch handler throughput: %s/sec%n", throughput);
	}

	@Test
	public void testSingleMessageHandlerThroughput() {
		output.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				counter.incrementAndGet();
			}
		});
		Message<?> msg = new GenericMessage<Object>("Hello World!");

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < TIMEOUT) {
			output.send(msg);
		}
		long end = System.currentTimeMillis();
		double elapsed = end - start;
		int throughput = (int)(counter.get() / (elapsed / 1000));

		System.out.format("single handler throughput: %s/sec%n", throughput);
	}

	@Test
	public void testBatchMessageHandlerSerializerThroughput() {
		final Kryo kryo = new Kryo();
		final Output kryoOut = new Output(new NullOutputStream());

		output.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				kryo.writeObject(kryoOut, message);
				if(List.class.isInstance(message.getPayload())) {
					counter.addAndGet(((List)message.getPayload()).size());
				} else {
					counter.incrementAndGet();
				}
			}
		});

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < TIMEOUT) {
			Message<?> msg = new GenericMessage<String>("Hello World!");
			batcher.handleMessage(msg);
		}
		long end = System.currentTimeMillis();
		double elapsed = end - start;
		int throughput = (int)(counter.get() / (elapsed / 1000));

		System.out.format("serializing kryo handler throughput: %s/sec%n", throughput);
	}

	@Configuration
	static class TestConfig {

		@Bean
		public PublishSubscribeChannel output() {
			return new PublishSubscribeChannel();
		}

		@Bean
		public RingBufferMessageBatcher batcher(MessageChannel output) {
			RingBufferMessageBatcher batcher = new RingBufferMessageBatcher(1024);
			batcher.setOutputChannel(output);
			return batcher;
		}

	}

}
