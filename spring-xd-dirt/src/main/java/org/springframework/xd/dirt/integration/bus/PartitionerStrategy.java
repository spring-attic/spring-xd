/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus;


/**
 *
 * Strategy for determining the partition to which a message should be sent.
 * 
 * @author Gary Russell
 */
public interface PartitionerStrategy {

	/**
	 * Determine the partition based on a key.
	 * @param key The key.
	 * @param divisor 1 greater than the maximum value that can be returned.
	 * @return The partition
	 */
	int partition(Object key, int divisor);

}
