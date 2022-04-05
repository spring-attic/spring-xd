/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.client.impl;

import org.springframework.xd.rest.client.RichGaugeOperations;
import org.springframework.xd.rest.domain.metrics.RichGaugeResource;

/**
 * Implementation of the RichGauge part of the metrics API.
 *
 * @author Ilayaperumal Gopinathan
 * @author Paul Harris
 */
public class RichGaugeTemplate extends AbstractSingleMetricTemplate<RichGaugeResource> implements RichGaugeOperations {

	public RichGaugeTemplate(AbstractTemplate abstractTemplate) {
		super(abstractTemplate, "rich-gauges", RichGaugeResource.class);
	}

}
