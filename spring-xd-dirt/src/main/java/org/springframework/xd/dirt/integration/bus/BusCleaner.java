/*
 * Copyright 2015 the original author or authors.
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

import java.util.List;


/**
 * Interface for implementations that perform cleanup for message buses.
 *
 * @author Gary Russell
 */
public interface BusCleaner {

	/**
	 * Clean up all resources for the supplied stream.
	 * @param stream The stream.
	 * @return A list of resources removed.
	 */
	List<String> clean(String stream);

}
