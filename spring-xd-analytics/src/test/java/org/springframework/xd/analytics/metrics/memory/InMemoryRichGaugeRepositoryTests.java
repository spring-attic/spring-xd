/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.memory;

import org.springframework.xd.analytics.metrics.AbstractRichGaugeRepositoryTests;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;

/**
 * @author Luke Taylor
 */
public class InMemoryRichGaugeRepositoryTests extends AbstractRichGaugeRepositoryTests {

	@Override
	protected RichGaugeRepository createService() {
		return new InMemoryRichGaugeRepository();
	}
}
