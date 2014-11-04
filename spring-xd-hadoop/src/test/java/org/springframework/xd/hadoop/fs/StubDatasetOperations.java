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

import java.util.Collection;

import org.kitesdk.data.DatasetDescriptor;

import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.data.hadoop.store.dataset.DatasetRepositoryCallback;
import org.springframework.data.hadoop.store.dataset.RecordCallback;
import org.springframework.data.hadoop.store.dataset.ViewCallback;

/**
 * @author Thomas Risberg
 */
public class StubDatasetOperations implements DatasetOperations {

	@Override
	public <T> void read(Class<T> targetClass, RecordCallback<T> callback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> Collection<T> read(Class<T> targetClass) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> void read(Class<T> targetClass, RecordCallback<T> callback, ViewCallback viewCallback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> Collection<T> read(Class<T> targetClass, ViewCallback viewCallback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> void write(Collection<T> records) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void execute(DatasetRepositoryCallback callback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> DatasetDescriptor getDatasetDescriptor(Class<T> targetClass) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> String getDatasetName(Class<T> clazz) {
		throw new UnsupportedOperationException("not implemented");
	}

}
