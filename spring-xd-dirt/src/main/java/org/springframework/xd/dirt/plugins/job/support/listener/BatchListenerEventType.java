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

package org.springframework.xd.dirt.plugins.job.support.listener;


/**
 * Enumerations to help identify various Batch job events being issued.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public enum BatchListenerEventType {

	JOB_EXECUTION_LISTENER_BEFORE_JOB,
	JOB_EXECUTION_LISTENER_AFTER_JOB,

	STEP_EXECUTION_LISTENER_BEFORE_STEP,
	STEP_EXECUTION_LISTENER_AFTER_STEP,

	CHUNK_LISTENER_AFTER_CHUNK,
	CHUNK_LISTENER_BEFORE_CHUNK,
	CHUNK_LISTENER_AFTER_CHUNK_ERROR,

	ITEM_LISTENER_ON_READ_ERROR,
	ITEM_LISTENER_ON_PROCESS_ERROR,
	ITEM_LISTENER_AFTER_WRITE,
	ITEM_LISTENER_ON_WRITE_ERROR,

	SKIP_LISTENER_ON_SKIP_IN_READ,
	SKIP_LISTENER_ON_SKIP_IN_WRITE,
	SKIP_LISTENER_ON_SKIP_IN_PROCESS

}
