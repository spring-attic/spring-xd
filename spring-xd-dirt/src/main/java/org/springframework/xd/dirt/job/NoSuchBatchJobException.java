/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.job;

/**
 * Exception thrown when there is no such batch job with the given name.
 * 
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class NoSuchBatchJobException extends JobException {

	public NoSuchBatchJobException(String name) {
		super("Batch Job with the name " + name + " doesn't exist");
	}

	public NoSuchBatchJobException(String idName, String id, Throwable e) {
		super("Batch Job with " + idName + " " + id + " doesn't exist", e);
	}

	public NoSuchBatchJobException(String name, Throwable e) {
		super("Batch Job with the name " + name + " doesn't exist", e);
	}
}
