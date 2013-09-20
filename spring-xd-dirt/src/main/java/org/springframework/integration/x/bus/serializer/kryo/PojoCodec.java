/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.x.bus.serializer.kryo;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer that can handle arbitrary types
 * 
 * @author David Turanski
 * @since 1.0
 */
public class PojoCodec extends AbstractKryoMultiTypeCodec<Object> {

	@Override
	protected void doSerialize(Object object, Kryo kryo, Output output) {
		kryo.register(object.getClass());
		kryo.writeObject(output, object);
	}

	@Override
	protected Object doDeserialize(Kryo kryo, Input input, Class<? extends Object> type) {
		kryo.register(type);
		return kryo.readObject(input, type);
	}


	@Override
	protected synchronized Kryo getKryoInstance() {
		Kryo kryo = new Kryo();
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		return kryo;
	}
}
