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

package org.springframework.xd.dirt.core;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * Builder object for paths under {@link Paths#JOBS}. {@code JobsPath} can be used to take a full path and split it into
 * its elements, for example:
 * <p>
 * <code>
 * JobsPath jobsPath = new JobsPath("/xd/jobs/my-job");
 * assertEquals("my-job", jobsPath.getJobName());
 * </code>
 * </p>
 * It can also be used to build a path, for example:
 * <p>
 * <code>
 * JobsPath jobsPath = new JobsPath().setJobName("my-job");
 * assertEquals("/jobs/my-job", jobsPath.build());
 * </code>
 * </p>
 * 
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class JobsPath {

	/**
	 * Index for {@link Paths#JOBS} in {@link #elements} array.
	 */
	private static final int JOBS = 0;

	/**
	 * Index for job name in {@link #elements} array.
	 */
	private static final int JOB_NAME = 1;

	/**
	 * Index for module label in {@link #elements} array.
	 */
	private static final int MODULE_LABEL = 2;

	/**
	 * Index for container name in {@link #elements} array.
	 */
	private static final int CONTAINER = 3;

	/**
	 * Array of path elements.
	 */
	private final String[] elements = new String[4];


	/**
	 * Construct a {@code JobsPath}. Use of this constructor means that a path will be created via {@link #build()} or
	 * {@link #buildWithNamespace()}.
	 */
	public JobsPath() {
		elements[JOBS] = Paths.JOBS;
	}

	/**
	 * Construct a {@code JobsPath}. Use of this constructor means that an existing path will be provided and this
	 * object will be used to extract the individual elements of the path. Both full paths (including and excluding the
	 * {@link Paths#XD_NAMESPACE XD namespace prefix}) are supported.
	 * 
	 * @param path job path
	 */
	public JobsPath(String path) {
		Assert.hasText(path);

		String[] pathElements = path.split("\\/");

		// offset is the element array that contains the 'jobs'
		// path element; the location may vary depending on whether
		// the path string includes the '/xd' namespace
		int offset = -1;
		for (int i = 0; i < pathElements.length; i++) {
			if (pathElements[i].equals(Paths.JOBS)) {
				offset = i;
				break;
			}
		}

		if (offset == -1) {
			throw new IllegalArgumentException(String.format(
					"Path '%s' does not include a '%s' element", path, Paths.JOBS));
		}

		System.arraycopy(pathElements, offset, elements, 0, pathElements.length - offset);

		Assert.state(elements[JOBS].equals(Paths.JOBS));
	}

	/**
	 * Return the job name.
	 * 
	 * @return job name
	 */
	public String getJobName() {
		return elements[JOB_NAME];
	}

	/**
	 * Set the job name.
	 * 
	 * @param name job name
	 * 
	 * @return this object
	 */
	public JobsPath setJobName(String name) {
		elements[JOB_NAME] = name;
		return this;
	}

	/**
	 * Return the module label.
	 * 
	 * @return module label
	 */
	public String getModuleLabel() {
		return elements[MODULE_LABEL];
	}

	/**
	 * Set the module label.
	 * 
	 * @param label module label
	 * 
	 * @return this object
	 */
	public JobsPath setModuleLabel(String label) {
		elements[MODULE_LABEL] = label;
		return this;
	}

	/**
	 * Return the container name.
	 * 
	 * @return container name
	 */
	public String getContainer() {
		return elements[CONTAINER];
	}

	/**
	 * Set the container name.
	 * 
	 * @param container container name
	 * 
	 * @return this object
	 */
	public JobsPath setContainer(String container) {
		elements[CONTAINER] = container;
		return this;
	}

	/**
	 * Build the path string using the field values.
	 * 
	 * @return path string
	 * 
	 * @see Paths#build
	 */
	public String build() {
		return Paths.build(stripNullElements());
	}

	/**
	 * Build the path string using the field values, including the namespace prefix.
	 * 
	 * @return path string with namespace
	 * 
	 * @see Paths#buildWithNamespace
	 */
	public String buildWithNamespace() {
		return Paths.buildWithNamespace(stripNullElements());
	}

	/**
	 * Return an array omitting the null values in {@link #elements}.
	 * 
	 * @return {@code elements} array without null values
	 */
	protected String[] stripNullElements() {
		int i = elements.length;

		// assuming that 'i' will never be < 0 because
		// both constructors assign a value to elements[0]
		while (elements[i - 1] == null) {
			--i;
		}
		if (i == elements.length) {
			return elements;
		}
		String[] s = new String[i];
		System.arraycopy(elements, 0, s, 0, i);
		return s;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return build();
	}

}
