/*
 * Copyright 2014 the original author or authors.
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

import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.springframework.xd.dirt.integration.bus.serializer.kryo.KryoRegistrar } implementation backed by a Map
 * used to explicitly set the registration ID for each class.
 *
 * @author David Turanski
 * @since 1.1
 */
public class KryoClassMapRegistrar implements KryoRegistrar {

	private final static Logger log = LoggerFactory.getLogger(KryoClassMapRegistrar.class);

	final private Map<Integer, Class<?>> kryoRegisteredClasses;

	public KryoClassMapRegistrar(Map<Integer, Class<?>> kryoRegisteredClasses) {
		this.kryoRegisteredClasses = kryoRegisteredClasses;
	}

	@Override
	public void registerTypes(Kryo kryo) {
		if (kryoRegisteredClasses == null) {
			return;
		}
		for (Map.Entry<Integer, Class<?>> entry : kryoRegisteredClasses.entrySet()) {
			if (log.isDebugEnabled()) {
				log.debug("registering class {} id = {}", entry.getValue(), entry.getKey());
			}
			kryo.register(entry.getValue(), entry.getKey());
		}
	}
}
