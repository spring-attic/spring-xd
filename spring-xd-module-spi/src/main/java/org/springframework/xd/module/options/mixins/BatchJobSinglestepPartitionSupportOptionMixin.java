/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.module.options.mixins;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Captures options made available via the import of {@code singlestep-partition-support.xml}.
 *
 * @author Eric Bottard
 */
@Mixin(BatchJobRestartableOptionMixin.class)
public class BatchJobSinglestepPartitionSupportOptionMixin {

	private long partitionResultsTimeout = 3_600_000; // 1 hour

	public long getPartitionResultsTimeout() {
		return partitionResultsTimeout;
	}

	@ModuleOption("time (ms) that the partition handler will wait for results")
	public void setPartitionResultsTimeout(long partitionResultsTimeout) {
		this.partitionResultsTimeout = partitionResultsTimeout;
	}




}
