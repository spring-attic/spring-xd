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
	public Future<JpmmlAnalyticalModel> requestModelUpdate(String id) {
		return taskExecutor.submit(newModelBuildingTask());
	}

	protected Callable<JpmmlAnalyticalModel> newModelBuildingTask() {
		return new Callable<JpmmlAnalyticalModel>() {
			@Override
			public JpmmlAnalyticalModel call() throws Exception {
				return modelBuilder.buildModel();
			}
		};
	}
}
