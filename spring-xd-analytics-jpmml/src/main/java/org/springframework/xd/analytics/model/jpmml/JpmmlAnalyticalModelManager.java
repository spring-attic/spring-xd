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
package org.springframework.xd.analytics.model.jpmml;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.xd.analytics.model.AnalyticalModelManager;
import org.springframework.xd.analytics.model.AnalyticalModelRepository;

/**
 * Author: Thomas Darimont
 */
public class JpmmlAnalyticalModelManager implements AnalyticalModelManager<JpmmlAnalyticalModel> {

	@Autowired
	private AnalyticalModelRepository<JpmmlAnalyticalModel> modelRepository;

	private JpmmlAnalyticalModelBuilder modelBuilder;

	private AsyncTaskExecutor taskExecutor;

	@Override
	public JpmmlAnalyticalModel getModel(String id) {
		return modelRepository.getById(id);
	}

	@Override
	public Future<JpmmlAnalyticalModel> requestModelUpdate(String name) {
		return taskExecutor.submit(newModelBuildingTask(name));
	}

	protected Callable<JpmmlAnalyticalModel> newModelBuildingTask(final String name) {
		return new Callable<JpmmlAnalyticalModel>() {
			@Override
			public JpmmlAnalyticalModel call() throws Exception {
				return modelBuilder.buildModel(name);
			}
		};
	}
}
