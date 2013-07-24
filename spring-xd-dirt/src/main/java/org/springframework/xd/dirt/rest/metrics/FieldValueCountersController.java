/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.rest.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.rest.client.domain.metrics.FieldValueCounterResource;

/**
 * Controller that exposes {@link FieldValueCounter} related representations.
 * 
 * @author Eric Bottard
 */
// @Controller
@RequestMapping("/metrics/field-value-counters")
@ExposesResourceFor(FieldValueCounterResource.class)
public class FieldValueCountersController extends
		AbstractMetricsController<FieldValueCounterRepository, FieldValueCounter> {

	@Autowired
	public FieldValueCountersController(FieldValueCounterRepository repository) {
		super(repository);
	}

}
