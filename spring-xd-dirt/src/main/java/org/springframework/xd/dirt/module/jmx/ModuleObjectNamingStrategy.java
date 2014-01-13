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

package org.springframework.xd.dirt.module.jmx;

import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;


/**
 * 
 * @author David Turanski
 */
public class ModuleObjectNamingStrategy implements ObjectNamingStrategy {

	private final Properties objectNameProperties;

	private final String domain;

	public ModuleObjectNamingStrategy(String domain, Properties objectNameProperties) {
		this.domain = domain;
		this.objectNameProperties = objectNameProperties;
		Assert.hasText(domain, "domain cannot be null or empty");
		Assert.notNull(objectNameProperties, "moduleProperties cannot be null");
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		System.out.println("objectName:" + beanKey);
		ObjectName originalName = ObjectNameManager.getInstance(beanKey);
		StringBuilder sb = new StringBuilder();
		sb.append(domain).append(":");
		sb.append("module=").append(objectNameProperties.get("module")).append(".").append(
				objectNameProperties.getProperty("index")).append(",");
		sb.append("component=").append(originalName.getKeyProperty("type")).append(",");
		sb.append("name=").append(originalName.getKeyProperty("name"));
		System.out.println(sb.toString());
		return ObjectNameManager.getInstance(sb.toString());
	}
}
