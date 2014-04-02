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

package org.springframework.xd.hadoop.fs;

import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.util.Assert;

/**
 * 
 * @author Thomas Risberg
 */
public class DatasetWriterFactory implements HdfsWriterFactory {

	private DatasetOperations datasetOperations;

	public DatasetWriterFactory(DatasetOperations datasetOperations) {
		Assert.notNull(datasetOperations, "DatasetTemplate must not be null.");
		this.datasetOperations = datasetOperations;
	}

	@Override
	public HdfsWriter createWriter() {
		DatasetWriter writer = new DatasetWriter(datasetOperations);
		return writer;
	}

}
