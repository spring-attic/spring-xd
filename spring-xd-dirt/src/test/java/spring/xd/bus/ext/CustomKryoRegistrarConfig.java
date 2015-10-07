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

package spring.xd.bus.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.codec.kryo.AbstractKryoRegistrar;
import org.springframework.integration.codec.kryo.KryoRegistrationRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author David Turanski
 * @author Gary Russell
 */
@Profile("kryo-test")
@Configuration
public class CustomKryoRegistrarConfig {

	@Bean
	public AbstractKryoRegistrar myCustomSerializer() {
		List<Registration> registrations = new ArrayList<>();
		registrations.add(new Registration(MyObject.class, new MySerializer(), 62));
		return new KryoRegistrationRegistrar(registrations);
	}

	public static class MyObject {
	}

	public static class MySerializer extends Serializer<MyObject> {

		@Override
		public void write(Kryo kryo, Output output, MyObject object) {
		}

		@Override
		public MyObject read(Kryo kryo, Input input, Class<MyObject> type) {
			return null;
		}
	}

}
