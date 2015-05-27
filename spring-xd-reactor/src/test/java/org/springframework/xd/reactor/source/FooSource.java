package org.springframework.xd.reactor.source;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.xd.reactor.EnableReactorModule;
import org.springframework.xd.reactor.ReactiveOutput;
import org.springframework.xd.reactor.ReactiveSource;
import reactor.fn.Function;
import reactor.rx.Streams;

/**
 * @author Stephane Maldini
 */
@Profile({"foo-source", "source-and-sink"})
@EnableReactorModule(output = "outputSource")
public class FooSource implements ReactiveSource<String> {

	@Bean
	public PollableChannel outputSource() {
		return new QueueChannel(20);
	}

	@Override
	public void accept(ReactiveOutput<String> output) {
		output.writeOutput(
				Streams
						.range(0, 19)
						.map(new Function<Long, String>() {
							@Override
							public String apply(Long aLong) {
								return aLong.toString();
							}
						})
		);
	}
}
