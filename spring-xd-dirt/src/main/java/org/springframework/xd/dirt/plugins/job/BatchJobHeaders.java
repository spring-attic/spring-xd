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

package org.springframework.xd.dirt.plugins.job;

/**
 * Headers definitions used by the batch job plugin.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public final class BatchJobHeaders {

	public static final String BATCH_LISTENER_EVENT_TYPE = "xd_batch_listener_event_type";

	public static final String BATCH_EXCEPTION = "xd_batch_exception";

	private BatchJobHeaders() {
	}

}
