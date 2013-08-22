/*
 * Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor.dispatcher;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.dispatcher.MessageDispatcher;
import reactor.core.processor.Operation;
import reactor.core.processor.Processor;
import reactor.core.processor.spec.ProcessorSpec;
import reactor.function.Consumer;
import reactor.function.Supplier;
import reactor.function.support.DelegatingConsumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jon Brisbin
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReactorProcessorMessageDispatcher implements MessageDispatcher {

	private final Map<MessageHandler, Consumer>    messageHandlerConsumers = new ConcurrentHashMap<MessageHandler, Consumer>();
	private final DelegatingConsumer<MessageEvent> delegatingConsumer      = new DelegatingConsumer<MessageEvent>();
	private final Processor<MessageEvent> processor;

	public ReactorProcessorMessageDispatcher() {
		this(false);
	}

	public ReactorProcessorMessageDispatcher(boolean singleThreadedProducer) {
		ProcessorSpec<MessageEvent> spec = new ProcessorSpec<MessageEvent>()
				.dataSupplier(new Supplier<MessageEvent>() {
					@Override
					public MessageEvent get() {
						return new MessageEvent();
					}
				})
				.consume(delegatingConsumer);
		if (singleThreadedProducer) {
			spec.singleThreadedProducer();
		} else {
			spec.multiThreadedProducer();
		}
		this.processor = spec.get();
	}

	@Override
	public boolean addHandler(final MessageHandler handler) {
		Consumer<MessageEvent> consumer = new Consumer<MessageEvent>() {
			@Override
			public void accept(MessageEvent ev) {
				handler.handleMessage(ev.message);
			}
		};
		messageHandlerConsumers.put(handler, consumer);
		delegatingConsumer.add(consumer);
		return true;
	}

	@Override
	public boolean removeHandler(MessageHandler handler) {
		Consumer<MessageEvent> consumer = messageHandlerConsumers.remove(handler);
		if (null == consumer) {
			return false;
		}
		delegatingConsumer.remove(consumer);
		return true;
	}

	@Override
	public boolean dispatch(Message<?> message) {
		Operation<MessageEvent> op = processor.prepare();
		op.get().message = message;
		op.commit();
		return true;
	}

	private static class MessageEvent {
		Message<?> message;
	}

}
