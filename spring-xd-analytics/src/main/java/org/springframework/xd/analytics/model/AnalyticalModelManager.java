/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.xd.analytics.model;

import java.util.concurrent.Future;

/**
 * Manages the life-cycle of an {@code AnalyticalModel}.
 *
 * Author: Thomas Darimont
 */
public interface AnalyticalModelManager<M extends AnalyticalModel>{

	/**
	 * @param id
	 * @return the model with the given {@code id}.
	 */
	M getModel(String id);

	/**
	 * Triggers an update for the model with the given {@code name}.
	 *
	 * @param name
	 * @return a {@link Future} that will eventually hold the updated model instance.
	 */
	Future<M> requestModelUpdate(String name);
}
