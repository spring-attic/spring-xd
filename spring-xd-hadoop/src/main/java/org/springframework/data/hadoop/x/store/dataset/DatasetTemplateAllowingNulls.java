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

package org.springframework.data.hadoop.x.store.dataset;

import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetNotFoundException;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.PartitionStrategy;
import org.springframework.data.hadoop.store.dataset.DatasetRepositoryFactory;
import org.springframework.data.hadoop.store.dataset.DatasetTemplate;

import java.util.Collection;

/**
 * TODO: remove this class once we update to use spring-data-hadoop 2.0.0.M5
 *
 * Temporary patch class to allow POJOs containing null values to be stored in Datasets
 *
 * @author Thomas Risberg
 */
public class DatasetTemplateAllowingNulls extends DatasetTemplate {

	private DatasetRepositoryFactory dsFactory;

	/**
	 * The {@link DatasetRepositoryFactory} to use for this template.
	 *
	 * @param datasetRepositoryFactory the DatasetRepositoryFactory to use
	 */
	public void setDatasetRepositoryFactory(DatasetRepositoryFactory datasetRepositoryFactory) {
		this.dsFactory = datasetRepositoryFactory;
		super.setDatasetRepositoryFactory(datasetRepositoryFactory);
	}

	@Override
	public void write(Collection<?> records) {
		write(records, null);
	}

	@Override
	public void write(Collection<?> records, PartitionStrategy partitionStrategy) {
		if (records == null || records.size() < 1) {
			return;
		}
		@SuppressWarnings("unchecked")
		Class recordClass = (Class) records.iterator().next().getClass();
		Dataset dataset = getOrCreateDataset(recordClass, partitionStrategy);
		DatasetWriter writer = dataset.newWriter();
		try {
			writer.open();
			for (Object record : records) {
				writer.write(record);
			}
		}
		finally {
			writer.close();
		}
	}

	private <T> Dataset<T> getOrCreateDataset(Class<T> clazz, PartitionStrategy partitionStrategy) {
		String repoName = getDatasetName(clazz);
		Dataset<T> dataset;
		try {
			dataset = dsFactory.getDatasetRepository().load(repoName);
		}
		catch (DatasetNotFoundException ex) {
			Schema schema = ReflectData.AllowNull.get().getSchema(clazz);
			DatasetDescriptor descriptor;
			if (partitionStrategy == null) {
				descriptor = new DatasetDescriptor.Builder().schema(schema).build();
			}
			else {
				descriptor =
						new DatasetDescriptor.Builder().schema(schema).partitionStrategy(partitionStrategy).build();
			}
			dataset = dsFactory.getDatasetRepository().create(repoName, descriptor);
		}
		return dataset;
	}

}
