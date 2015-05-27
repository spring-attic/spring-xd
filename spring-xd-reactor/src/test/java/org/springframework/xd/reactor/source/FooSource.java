package org.springframework.xd.reactor.source;

import org.reactivestreams.Subscriber;
import org.springframework.context.annotation.Profile;
import org.springframework.xd.reactor.ReactiveSource;
import org.springframework.xd.reactor.EnableReactorModule;
import reactor.fn.Function;
import reactor.fn.Supplier;
import reactor.rx.Streams;

/**
 * @author Stephane Maldini
 */
@Profile("foo-source")
@EnableReactorModule
public class FooSource implements ReactiveSource<String> {
	@Override
	public void accept(Supplier<Subscriber<String>> subscriberSupplier) {
		Streams
				.range(0, 19)
				.map(new Function<Long, String>() {
					@Override
					public String apply(Long aLong) {
						return aLong.toString();
					}
				})
				.subscribe(subscriberSupplier.get());
	}
}
