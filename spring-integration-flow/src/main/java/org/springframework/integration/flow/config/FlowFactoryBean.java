/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config;

import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.flow.Flow;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @since 3.0
 *
 */
public class FlowFactoryBean implements FactoryBean<Flow>, InitializingBean {
	private Flow flow;
	private String[] activeProfiles;
	private List<Resource> additionalComponentLocations;
	private Properties properties;
	private String moduleName;
	private String id;
	
	@Override
	public Flow getObject() throws Exception {
		return flow;
	}

	@Override
	public Class<?> getObjectType() {
		return Flow.class;
	}


	@Override
	public boolean isSingleton() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.id,"'id' property is required");
		if (!StringUtils.hasText(this.moduleName)) {
			this.moduleName = this.id;
		}
		this.flow = new Flow(this.moduleName);
		this.flow.setInstanceId(this.id);
		if (this.activeProfiles !=null && this.activeProfiles.length > 0) {
			this.flow.activateProfiles(this.activeProfiles);
		}
		if (!CollectionUtils.isEmpty(this.properties)) {
			this.flow.addProperties(this.properties);
		}
		if (!CollectionUtils.isEmpty(this.additionalComponentLocations)){
			this.flow.setAdditionalComponentLocations(this.additionalComponentLocations);
		}
	}

	/**
	 * @param flow the flow to set
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	/**
	 * @param activeProfiles the activeProfiles to set
	 */
	public void setActiveProfiles(String[] activeProfiles) {
		this.activeProfiles = activeProfiles;
	}

	/**
	 * @param additionalComponentLocations the additionalComponentLocations to set
	 */
	public void setAdditionalComponentLocations(List<Resource> additionalComponentLocations) {
		this.additionalComponentLocations = additionalComponentLocations;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * @param moduleName the moduleName to set
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
