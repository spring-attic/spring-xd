/*
 * Copyright 2013-2016 the original author or authors.
 *
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.mixins.MappedRequestHeadersMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * Captures options to the {@code rabbit} sink module.
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
@Mixin({ RabbitConnectionMixin.class, MappedRequestHeadersMixin.Amqp.class })
public class RabbitSinkOptionsMetadata {

	private String exchange = "";

	private String routingKey = "'" + ModulePlaceholders.XD_STREAM_NAME + "'";

	private String deliveryMode = "PERSISTENT";

	private String converterClass = "org.springframework.amqp.support.converter.SimpleMessageConverter";

	private String channelCacheSize = "${spring.rabbitmq.channelCacheSize}";

	public String getExchange() {
		return exchange;
	}

	@ModuleOption("the Exchange on the RabbitMQ broker to which messages should be sent")
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	@ModuleOption("the routing key to be passed with the message, as a SpEL expression")
	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public String getDeliveryMode() {
		return deliveryMode;
	}

	@ModuleOption("the delivery mode (PERSISTENT, NON_PERSISTENT)")
	public void setDeliveryMode(String deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public String getConverterClass() {
		return converterClass;
	}

	@ModuleOption("the class name of the message converter")
	public void setConverterClass(String converterClass) {
		this.converterClass = converterClass;
	}

	public String getChannelCacheSize() {
		return this.channelCacheSize;
	}

	@ModuleOption("the channel cache size")
	public void setChannelCacheSize(String channelCacheSize) {
		this.channelCacheSize = channelCacheSize;
	}

}
