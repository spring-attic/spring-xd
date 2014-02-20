package org.springframework.xd.integration.reactor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
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
	static int  MSG_COUNT = 4096;

	@Autowired
	RingBufferMessageBatcher batcher;
	@Autowired
	PublishSubscribeChannel  output;
	@Autowired
	AtomicLong               counter;
	@Autowired
	MessageHandler           counterHandler;

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

		assertThat("Latch was counted down", counter.get(), is(2l));
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
		output.subscribe(counterHandler);
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
				//counter.incrementAndGet();
				counter.addAndGet(((List)message.getPayload()).size());
			}
		});

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < TIMEOUT) {
			Message<?> msg = new GenericMessage<String>("Hello World!");
			//output.send(msg);
			batcher.handleMessage(msg);
		}
		long end = System.currentTimeMillis();
		double elapsed = end - start;
		int throughput = (int)(counter.get() / (elapsed / 1000));

		System.out.format("serializing handler throughput: %s/sec%n", throughput);
	}

	@Configuration
	static class TestConfig {

		@Bean
		public AtomicLong counter() {
			return new AtomicLong();
		}

		@Bean
		public MessageHandler counterHandler(final AtomicLong counter) {
			return new MessageHandler() {
				@Override
				public void handleMessage(Message<?> message) throws MessagingException {
					counter.incrementAndGet();
				}
			};
		}

		@Bean
		@Qualifier("optimizedOutput")
		public PublishSubscribeChannel optimizedOutput(final MessageHandler counterHandler) {
			final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher() {
				@Override
				public boolean dispatch(Message<?> message) {
					counterHandler.handleMessage(message);
					return true;
				}
			};
			return new PublishSubscribeChannel() {
				@Override
				protected BroadcastingDispatcher getDispatcher() {
					return dispatcher;
				}
			};
		}

		@Bean
		@Qualifier("output")
		public PublishSubscribeChannel output() {
			return new PublishSubscribeChannel();
		}

		@Bean
		public MessageChannel input() {
			return new DirectChannel();
		}

		@Bean
		public RingBufferMessageBatcher batcher(@Qualifier("output") MessageChannel output) {
			RingBufferMessageBatcher batcher = new RingBufferMessageBatcher(MSG_COUNT / 2);
			batcher.setOutputChannel(output);
			return batcher;
		}
	}

}
