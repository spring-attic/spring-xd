/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.server;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * Beans defined and imported here are in the global parent context, hence available to the entire hierarchy, including
 * Admins, Containers, and Modules.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
@EnableAutoConfiguration(exclude = {ServerPropertiesAutoConfiguration.class, BatchAutoConfiguration.class,
		ThymeleafAutoConfiguration.class, JmxAutoConfiguration.class, HealthIndicatorAutoConfiguration.class,
		AuditAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
		SolrAutoConfiguration.class })
@ImportResource("classpath:" + ConfigLocations.XD_CONFIG_ROOT + "global/parent-context.xml")
@EnableBatchProcessing
public class ParentConfiguration {

	@Bean
	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	public MBeanServerFactoryBean mbeanServer() {
		MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		return factoryBean;
	}

	@Bean
	@ConditionalOnExpression("${endpoints.health.enabled:true}")
	public ApplicationHealthIndicator healthIndicator() {
		return new ApplicationHealthIndicator();
	}

}
