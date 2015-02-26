/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

/**
 * Strategy interface used by {@link org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec} to register
 * classes consistently across {@link Kryo} instances. An XD user may register an instance of this type in the Spring XD
 * Application Context to enable kryo class registration which results in efficiency gains if you know the types your
 * application needs in advance. Note that Kryo serialization only applies to types used as message payloads in XD
 * streams.
 * By default, user defined types are not registered to Kryo. Registration allows a unique ID (small positive integer is
 * ideal) to represent the type in the byte stream. In a distributed environment, all Kryo instances must maintain the
 * same registration state in order to properly take advantage of this feature.
 * This is can result in better performance in demanding situations, but requires some care to maintain. Only use this
 * if you really need it. Otherwise, it is a great example of premature optimization.
 * This interface applies a strategy to register a statically configured, one-to-one mapping of a Java type to an
 * integer. Basic implementations are provided backed by a Map<Integer,Class<?>> or a List<Class<?>>. These are simple
 * and require the user to manually configure a bean in each XD server and ensure that the configuration is always
 * consistent.*
 * The container looks in classpath*:META-INF/spring-xd/xd/bus/ext/*.xml for an instance of this type named
 * "kryoRegistrar". The KryoRegistrar provides the registration mapping and the strategy to apply the mapping to every
 * Kryo instance. Note that statically declared Java types must also be present in the XD class path (xd/lib) else the
 * container will fail to initialize. Only one instance may be registered and identically configured across all
 * containers.
 *
 * @author David Turanski
 * @since 1.1
 */
public interface KryoRegistrar {
	/**
	 * This method is invoked by the {@link org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec} and
	 * applied to the {@link Kryo} instance whenever one is provided. This is currently done using an object pool so it
	 * is inevitable that this method will be invoked repeatedly on the same instance. Kryo registration is idempotent,
	 * but this could become inefficient if registering a large amount of types.
	 *
	 * @param kryo the provided instance
	 */
	void registerTypes(Kryo kryo);
}
