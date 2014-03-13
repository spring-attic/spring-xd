/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.util.StringValueResolver;
import org.springframework.xd.integration.reactor.net.NetServerSourceOptionsMetadata;

/**
 * @author Jon Brisbin
 */
public class OptionsMetadataValueResolver implements StringValueResolver,
                                                     BeanFactoryAware {

	private final NetServerSourceOptionsMetadata options;

	public OptionsMetadataValueResolver(NetServerSourceOptionsMetadata options) {
		this.options = options;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		((AbstractBeanFactory)beanFactory).addEmbeddedValueResolver(this);
	}

	@Override
	public String resolveStringValue(String strVal) {
		if("${bind}".equals(strVal)) {
			return options.getBind();
		}
		return null;
	}

}
