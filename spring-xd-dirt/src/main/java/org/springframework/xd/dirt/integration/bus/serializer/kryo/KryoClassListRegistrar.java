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

import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.Assert;

/**
 * A {@link org.springframework.xd.dirt.integration.bus.serializer.kryo.KryoRegistrar} used to register a 
 * list of Java classes. This assigns a sequential registration ID starting with an initial value (50 by default), but
 * may be configured. This is easiest to set up but requires that every server node be configured with the identical 
 * list in the same order.
 *
 * @author David Turanski
 * @since 1.1
 */
public class KryoClassListRegistrar implements KryoRegistrar {
	private final static Logger log = LoggerFactory.getLogger(KryoClassListRegistrar.class);

	private final List<Class<?>> registeredClasses;

	private int initialValue = 50;

	/**
	 *  
	 * @param classes the list of classes to register
	 */
	public KryoClassListRegistrar(List<Class<?>> classes) {
		this.registeredClasses = classes;
	}

	/**
	 * Set the inital ID value. Classes in the list will be sequentially assigned an ID starting with this value
	 * (default is 50).
	 * 
	 *
	 * @param initialValue the initial value
	 */
	public void setInitialValue(int initialValue) {
		Assert.isTrue(initialValue > 10, "'initialValue' must be a value greater than 10");
		this.initialValue = initialValue;
	}

	@Override
	public void registerTypes(Kryo kryo) {
		if (registeredClasses == null) {
			return;
		}
		for (int i = 0; i < registeredClasses.size(); i++) {
			if (log.isDebugEnabled()) {
				log.debug("registering class {} id = {}", registeredClasses.get(i).getName() , (i + initialValue));
			}
			kryo.register(registeredClasses.get(i), i + initialValue);
		}
	}

}
