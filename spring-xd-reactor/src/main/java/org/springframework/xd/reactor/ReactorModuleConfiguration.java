package org.springframework.xd.reactor;

/**
 * @author Stephane Maldini
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import reactor.rx.Stream;

@Configuration
@EnableIntegration
public class ReactorModuleConfiguration implements BeanPostProcessor, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private BeanFactory beanFactory;

	@Bean
	MessageChannel input() {
		return new DirectChannel();
	}

	@Bean
	MessageChannel output() {
		return new DirectChannel();
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (ReactiveModule.class.isAssignableFrom(bean.getClass())) {

			if (ReactiveSink.class.isAssignableFrom(bean.getClass())) {
				final ReactiveSink sink = (ReactiveSink) bean;
				createMessageHandler(
						bean,
						new ReactiveProcessor<Object, Object>() {
							@Override
							public void accept(Stream<Object> objectStream, ReactiveOutput<Object> output) {
								sink.accept(objectStream);
							}
						},
						ReactorReflectionUtils.extractGeneric(bean, ReactiveSink.class),
						null
				);
				return bean;
			}

			if (ReactiveSource.class.isAssignableFrom(bean.getClass())) {
				final ReactiveSource source = (ReactiveSource) bean;
				createMessageHandler(
						bean,
						new ReactiveProcessor<Object, Object>() {
							@Override
							public void accept(Stream<Object> objectStream, ReactiveOutput<Object> output) {
								source.accept(output);
							}
						},
						null,
						ReactorReflectionUtils.extractGeneric(bean, ReactiveSource.class));
				return bean;
			}

			createMessageHandler(
					bean,
					(ReactiveProcessor) bean,
					ReactorReflectionUtils.extractGeneric(bean, ReactiveProcessor.class, 0),
					ReactorReflectionUtils.extractGeneric(bean, ReactiveProcessor.class, 1)
			);
		}
		return bean;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void createMessageHandler(Object bean, ReactiveProcessor reactiveProcessor, Class inputType, Class outputType) {

		EnableReactorModule ann = ReactorReflectionUtils.extractAnnotation(bean);

		final MessageChannel output;
		final SubscribableChannel input;
		final int concurrency;
		final String partitionExpression;
		final int backlog;
		final long shutdownTimeout;

		if (ann != null) {
			input = beanFactory.getBean(ann.input(), SubscribableChannel.class);
			output = beanFactory.getBean(ann.output(), MessageChannel.class);
			concurrency = ann.concurrency();
			partitionExpression = ann.partition();
			backlog = ann.backlog();
			shutdownTimeout = ann.shutdownTimeout();
		} else {
			try {
				input = (SubscribableChannel) input();
			} catch (ClassCastException cce) {
				throw new IllegalArgumentException("Input channel must be of type SubscribableChannel");
			}
			output = output();
			concurrency = 1;
			partitionExpression = "";
			backlog = 8192;
			shutdownTimeout = 5000L;
		}


		final MessageHandler handler;
		if (inputType != null &&
				!partitionExpression.isEmpty()) {

			handler = new PartitionedReactorMessageHandler<>(
					reactiveProcessor, inputType, outputType, output, partitionExpression, backlog, shutdownTimeout
			);

		} else {
			handler = new ReactorMessageHandler<>(
					reactiveProcessor, output, inputType, outputType, concurrency, backlog, shutdownTimeout
			);
		}

		if (SubscribableChannel.class.isAssignableFrom(input.getClass())) {
			EventDrivenConsumer consumer = new EventDrivenConsumer(input, handler);
			consumer.afterPropertiesSet();
			consumer.start();
		} else {
			throw new IllegalArgumentException("Input channel is not subscribable: " + input + " of type " + input.getClass());
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
