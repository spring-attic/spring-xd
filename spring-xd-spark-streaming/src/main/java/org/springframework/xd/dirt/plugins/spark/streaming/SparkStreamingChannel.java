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
package org.springframework.xd.dirt.plugins.spark.streaming;

import java.io.Serializable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionPlugin;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionSupport;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * The {@link org.springframework.integration.channel.DirectChannel} that provides support for
 * spark streaming configurations.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
public class SparkStreamingChannel extends DirectChannel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Configure message converter for this direct channel.
	 *
	 * @param contentType the content type to use
	 */
	protected void configureMessageConverter(MimeType contentType) {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {ConfigLocations.XD_CONFIG_ROOT + "plugins/module-type-conversion.xml"});
		ModuleTypeConversionPlugin typeConversionPlugin = (ModuleTypeConversionPlugin) context.getBean("moduleTypeConversionPlugin");
		ModuleTypeConversionSupport moduleTypeConversionSupport = typeConversionPlugin.getModuleTypeConversionSupport();
		moduleTypeConversionSupport.configureMessageConverters(this, contentType, Thread.currentThread().getContextClassLoader());
	}
}
