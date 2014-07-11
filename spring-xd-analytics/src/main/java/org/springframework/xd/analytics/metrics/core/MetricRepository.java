/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.metrics.core;

import org.springframework.data.repository.CrudRepository;

/**
 * A repository to save, delete and find Metric instances. Uses the Spring Data CrudRepository marker interface and
 * conventions for method names and behavior.
 * 
 * The name is the id and should be unique.
 * 
 * @author Mark Pollack
 * @author Luke Taylor
 */
public interface MetricRepository<M extends Metric> extends CrudRepository<M, String> {

}
