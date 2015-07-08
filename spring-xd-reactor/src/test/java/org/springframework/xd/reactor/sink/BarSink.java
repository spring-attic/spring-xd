package org.springframework.xd.reactor.sink;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.reactor.ReactiveSink;
import org.springframework.xd.reactor.EnableReactorModule;
import reactor.fn.Consumer;
import reactor.rx.Stream;

import java.util.concurrent.CountDownLatch;

/**
 * @author Stephane Maldini
 */
@Profile({"bar-sink", "source-and-sink"})
@EnableReactorModule(input = "inputSink")
public class BarSink implements ReactiveSink<String> {

	public final CountDownLatch latch = new CountDownLatch(10);

	@Bean
	public MessageChannel inputSink() {
		return new DirectChannel();
	}

	@Override
	public void accept(Stream<String> stringStream) {
		stringStream.consume(
				new Consumer<String>() {
					@Override
					public void accept(String s) {
						System.out.println(s);
						latch.countDown();
					}
				},
				new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) {
						throwable.printStackTrace();
					}
				},
				new Consumer<Void>() {
					@Override
					public void accept(Void aVoid) {
						System.out.println("complete");
					}
				}
		);
	}
}
