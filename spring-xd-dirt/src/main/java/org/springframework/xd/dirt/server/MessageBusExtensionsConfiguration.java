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

package org.springframework.xd.dirt.server;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.kryo.FileKryoRegistrar;
import org.springframework.integration.codec.kryo.KryoRegistrar;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.tuple.serializer.kryo.TupleKryoRegistrar;

/**
 * Configuration class for Codec and possibly other MessageBus extensions
 *
 * @author David Turanski
 * @since 1.2
 */
@ComponentScan(basePackages = {"spring.xd.bus.ext"})
@ImportResource("classpath*:" + ConfigLocations.XD_CONFIG_ROOT + "bus/ext/*.xml")
public class MessageBusExtensionsConfiguration {
	@Autowired
	ApplicationContext applicationContext;

	@Value("${xd.codec.kryo.references}")
	private boolean useReferences;

	@Bean
	@ConditionalOnMissingBean(name = "codec")
	public Codec codec() {
		Map<String, KryoRegistrar> kryoRegistrarMap = applicationContext.getBeansOfType(KryoRegistrar
				.class);
		return new PojoCodec(new ArrayList<>(kryoRegistrarMap.values()), useReferences);
	}

	@Bean
	public KryoRegistrar fileRegistrar() {
		return new FileKryoRegistrar();
	}

	@Bean
	public KryoRegistrar tupleRegistrar() {
		return new TupleKryoRegistrar();
	}
}
