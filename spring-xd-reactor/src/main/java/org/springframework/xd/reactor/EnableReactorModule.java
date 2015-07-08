package org.springframework.xd.reactor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.annotation.*;

/**
 * Add this annotation to an {@code Bean}, likely a {@link ReactiveModule} class to have
 * the imported Spring XD Reactor configuration
 *
 * @author Stephane Maldini
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ReactorModuleConfiguration.class)
@Inherited
@Configuration
public @interface EnableReactorModule {

	/**
	 * Define concurrent workers for the given module. E.g.
	 * A processor might scale-up by defining N > 1.
	 * This will result in the Reactive Module being invoked N times
	 */
	int concurrency() default 1;

	/**
	 * Define input channel
	 */
	String input() default "input";

	/**
	 * Define output channel
	 */
	String output() default "output";

	/**
	 * Contract for performing stream processing using on a given expression evaluated partition.
	 * This string will be evaluated by an internal {@link SpelExpressionParser}.
	 */
	String partition() default "";


	/**
	 * How long to wait before interrupting the processor on context shutdown.
	 */
	long shutdownTimeout() default 5000L;

	/**
	 * Message structure size assigned to the module for async handoff.
	 * The size is pre-allocated, one must be careful when using in conjonction with partition() as this will be replicated N times.
	 * When the backlog size is reached, and the subscriber is not draining fast enough, the producer will be paused in the messagehandler.
	 */
	int backlog() default 8192;
}
