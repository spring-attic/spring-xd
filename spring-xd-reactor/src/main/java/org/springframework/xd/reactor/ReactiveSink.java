/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.reactor;

import org.reactivestreams.Subscriber;
import reactor.fn.Consumer;
import reactor.rx.Stream;

/**
 * Contract for performing stream processing using Reactor within an XD sink module.
 * <p/>
 * This {@link Consumer} will receive a {@link Stream} on module initialization or on new partition
 * and eventually subscribe a {@link Subscriber} or compose directly using {@link Stream} API.
 * <p/>
 * The {@link Stream} will map {@link Subscriber#onNext(Object)} calls to receive operations on the message bus.
 *
 * @author Stephane Maldini
 */
public interface ReactiveSink<O> extends Consumer<Stream<O>>, ReactiveModule {
}
