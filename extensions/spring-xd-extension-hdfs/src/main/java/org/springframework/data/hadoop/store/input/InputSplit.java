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

package org.springframework.data.hadoop.store.input;

import org.apache.hadoop.fs.Path;

/**
 * {@code InputSplit} is a logical representation of a split for {@code Path}.
 * 
 * @author Janne Valkealahti
 * 
 */
public interface InputSplit {

	/**
	 * Gets the {@code Path} for an input split.
	 * 
	 * @return the input split path
	 */
	Path getPath();

	/**
	 * Gets the start of a split for {@code Path} returned from a {@link #getPath()}.
	 * 
	 * @return the start of an input split
	 */
	long getStart();

	/**
	 * Gets the length of a split for {@code Path} returned from a {@link #getPath()}.
	 * 
	 * @return the length of an input split
	 */
	long getLength();

	/**
	 * Gets the locations for splits. The actual returned value should be determined by the implementation. For example
	 * in terms of split for file in hdfs, locations would have information for raw hdfs file block locations.
	 * 
	 * @return the locations
	 */
	String[] getLocations();

}
