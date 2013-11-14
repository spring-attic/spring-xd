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

package org.springframework.data.hadoop.batch.store.item;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.util.Assert;

/**
 * An {@code ItemWriter} implementation that can handle writing POJOs as records in a dataset using a
 * {@code DatasetOperations}
 */
public class DatasetItemWriter<T> implements ItemWriter<T> {

	private DatasetOperations datasetOperations;

	/**
	 * The implementation of {@link DatasetOperations} to be used
	 * 
	 * @param datasetOperations
	 */
	public void setDatasetOperations(DatasetOperations datasetOperations) {
		this.datasetOperations = datasetOperations;
	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		Assert.notNull(datasetOperations, "The property datasetOperations can't be null.");
		datasetOperations.write(items);
	}
}
