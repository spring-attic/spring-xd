/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.xd.dirt.module.jmx;

import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;


/**
 * Object naming strategy for XD module.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 */
public class ModuleObjectNamingStrategy extends MetadataNamingStrategy {

	private final Properties objectNameProperties;

	private final String domain;

	public ModuleObjectNamingStrategy(String domain, Properties objectNameProperties) {
		this(new AnnotationJmxAttributeSource(), domain, objectNameProperties);
	}

	public ModuleObjectNamingStrategy(JmxAttributeSource attributeSource, String domain, Properties objectNameProperties) {
		super(attributeSource);
		Assert.hasText(domain, "domain cannot be null or empty");
		Assert.notNull(objectNameProperties, "moduleProperties cannot be null");
		this.domain = domain;
		this.objectNameProperties = objectNameProperties;
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		ObjectName originalName = super.getObjectName(managedBean, beanKey);
		StringBuilder sb = new StringBuilder();
		sb.append(this.domain).append(":");
		sb.append("module=").append(this.objectNameProperties.get("group")).append(".").append(
				this.objectNameProperties.getProperty("type")).append(".").append(
				this.objectNameProperties.getProperty("label")).append(".").append(
				this.objectNameProperties.getProperty("sequence")).append(",");
		sb.append("component=").append(originalName.getKeyProperty("type")).append(",");
		sb.append("name=").append(originalName.getKeyProperty("name"));
		return ObjectNameManager.getInstance(sb.toString());
	}

}
