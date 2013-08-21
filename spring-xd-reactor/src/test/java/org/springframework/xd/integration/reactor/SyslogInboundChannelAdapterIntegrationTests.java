package org.springframework.xd.integration.reactor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapter;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.spring.context.config.EnableReactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SyslogInboundChannelAdapterIntegrationTests {

	CountDownLatch latch;
	Environment    env;
	@Autowired
	SyslogInboundChannelAdapter channelAdapter;
	@Autowired
	SyslogWriter                syslogWriter;
	@Autowired
	DirectChannel               output;

	@Before
	public void setup() {
		env = new Environment();

		output.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		});

		latch = new CountDownLatch(1);
	}

	@Test
	public void testSyslogInboundChannelAdapter() throws InterruptedException {
		syslogWriter.run();

		assertTrue("Latch did not time out", latch.await(1, TimeUnit.SECONDS));
	}

	@Configuration
	@EnableReactor
	static class TestConfiguration {
		@Bean
		public Reactor reactor(Environment env) {
			return Reactors.reactor().env(env).get();
		}

		@Bean
		public DirectChannel output() {
			return new DirectChannel();
		}


		@Bean
		public SyslogInboundChannelAdapter syslogInboundChannelAdapter(Environment env, DirectChannel output) {
			SyslogInboundChannelAdapter sica = new SyslogInboundChannelAdapter(env);
			sica.setOutputChannel(output);
			return sica;
		}

		@Bean
		public SyslogWriter syslogWriter() {
			return new SyslogWriter();
		}
	}

	static class SyslogWriter extends Thread {
		@Override
		public void run() {
			try {
				SocketChannel channel = SocketChannel.open();
				channel.connect(new InetSocketAddress(5140));

				ByteBuffer buff = ByteBuffer.wrap("<34>Oct 11 22:14:15 mymachine su: 'su root' failed for lonvick on /dev/pts/8\n".getBytes());
				channel.write(buff);

				channel.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}
	}

}
