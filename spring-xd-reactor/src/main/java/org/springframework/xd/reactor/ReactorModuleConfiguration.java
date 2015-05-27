package org.springframework.xd.reactor;

/**
 * @author Stephane Maldini
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import reactor.fn.Supplier;
import reactor.rx.Stream;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Configuration
@EnableIntegration
public class ReactorModuleConfiguration implements BeanPostProcessor, BeanFactoryAware{

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
				createMessageHandler(new ReactiveProcessor<Object, Object>() {
					@Override
					public void accept(Stream<Object> objectStream, Supplier<Subscriber<Object>> subscriberSupplier) {
						sink.accept(objectStream);
					}
				}, extractGeneric(bean, ReactiveSink.class));
				return bean;
			}

			if (ReactiveSource.class.isAssignableFrom(bean.getClass())) {
				final ReactiveSource source = (ReactiveSource) bean;
				createMessageHandler(new ReactiveProcessor<Object, Object>() {
					@Override
					public void accept(Stream<Object> objectStream, Supplier<Subscriber<Object>> subscriberSupplier) {
						source.accept(subscriberSupplier);
					}
				}, null);
				return bean;
			}

			createMessageHandler((ReactiveProcessor) bean, extractGeneric(bean, ReactiveProcessor.class));
		}
		return bean;
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	private void createMessageHandler(ReactiveProcessor bean, Class inputType) {
		final AbstractMessageProducingHandler handler;
		if (inputType != null &&
				PartitionAware.class.isAssignableFrom(bean.getClass())) {

			String partitionExpression = ((PartitionAware)bean).get();
			handler = new PartitionedReactorMessageHandler<>(bean, inputType, partitionExpression);

		}else{
			handler = new ReactorMessageHandler<>(bean, inputType);
		}

		handler.setOutputChannel(output());
		handler.setBeanFactory(beanFactory);
		handler.afterPropertiesSet();

		MessageChannel input = input();
		if(SubscribableChannel.class.isAssignableFrom(input.getClass())){
			EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel)input, handler);
			consumer.afterPropertiesSet();
			consumer.start();
		}else{
			throw new IllegalArgumentException("Input channel is not subscribable: "+input+" of type "+input.getClass());
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

	@SuppressWarnings("unchecked")
	static <IN> Class<IN> extractGeneric(Object processor, Class<?> target) {
		Class<?> searchType = processor.getClass();
		while (searchType != Object.class) {
			if (searchType.getGenericInterfaces().length == 0){
				continue;
			}
			for (Type t : searchType.getGenericInterfaces()) {
				if (ParameterizedType.class.isAssignableFrom(t.getClass())
						&& ((ParameterizedType)t).getRawType().equals(target)) {
					ParameterizedType pt = (ParameterizedType) t;

					if (pt.getActualTypeArguments().length == 0) return (Class<IN>)Message.class;

					t = pt.getActualTypeArguments()[0];
					if (t instanceof ParameterizedType) {
						return (Class<IN>) ((ParameterizedType) t).getRawType();
					} else if (t instanceof Class) {
						return (Class<IN>) t;
					}
				}
			}
			searchType = searchType.getSuperclass();
		}
		return (Class<IN>)Message.class;
	}

}
