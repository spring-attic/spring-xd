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
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * Builder object for paths under {@link Paths#JOB_DEPLOYMENTS}. {@code JobDeploymentsPath}
 * can be used to take a full path and split it into its elements, for example:
 * <pre>
 * JobDeploymentsPath path = new JobDeploymentsPath("/xd/deployments/jobs/my-job");
 * assertEquals("my-job", path.getJobName());
 * </pre>
 * It can also be used to build a path, for example:
 * <pre>
 * JobDeploymentsPath path = new JobDeploymentsPath().setJobName("my-job");
 * assertEquals("/deployments/jobs/my-job", path.build());
 * </pre>
 * Note that when building a deployment path, if a module label is set,
 * a container must also be set (and vice versa).
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class JobDeploymentsPath {

	/**
	 * Index for {@link Paths#DEPLOYMENTS} in {@link #elements} array.
	 */
	private static final int DEPLOYMENTS = 0;

	/**
	 * Index for {@link Paths#JOB_DEPLOYMENTS} in {@link #elements} array.
	 */
	private static final int JOBS = 1;

	/**
	 * Index for job name in {@link #elements} array.
	 */
	private static final int JOB_NAME = 2;

	/**
	 * Index for dot delimited module deployment description in {@link #elements} array.
	 */
	private static final int DEPLOYMENT_DESC = 3;

	/**
	 * Index for module label in {@link #deploymentDesc} array.
	 */
	private static final int MODULE_LABEL = 0;

	/**
	 * Index for container name in {@link #deploymentDesc} array.
	 */
	private static final int CONTAINER = 1;

	/**
	 * Array of path elements.
	 */
	private final String[] elements = new String[4];

	/**
	 * Array of module deployment description elements.
	 */
	private final String[] deploymentDesc = new String[2];


	/**
	 * Construct a {@code JobDeploymentsPath}. Use of this constructor
	 * means that a path will be created via {@link #build()} or
	 * {@link #buildWithNamespace()}.
	 */
	public JobDeploymentsPath() {
		elements[DEPLOYMENTS] = Paths.DEPLOYMENTS;
		elements[JOBS] = Paths.JOBS;
	}

	/**
	 * Construct a {@code JobDeploymentsPath}. Use of this constructor
	 * means that an existing path will be provided and this object will
	 * be used to extract the individual elements of the path. Both full
	 * paths (including and excluding the
	 * {@link Paths#XD_NAMESPACE XD namespace prefix}) are supported.
	 *
	 * @param path job deployment path
	 */
	public JobDeploymentsPath(String path) {
		Assert.hasText(path);

		String[] pathElements = path.split("\\/");

		// offset is the element array that contains the 'deployments'
		// path element; the location may vary depending on whether
		// the path string includes the '/xd' namespace
		int offset = -1;
		for (int i = 0; i < pathElements.length; i++) {
			if (pathElements[i].equals(Paths.DEPLOYMENTS)) {
				offset = i;
				break;
			}
		}

		if (offset == -1) {
			throw new IllegalArgumentException(String.format(
					"Path '%s' does not include a '%s' element", path, Paths.DEPLOYMENTS));
		}

		System.arraycopy(pathElements, offset, elements, 0, pathElements.length - offset);

		Assert.state(elements[DEPLOYMENTS].equals(Paths.DEPLOYMENTS));
		Assert.state(elements[JOBS].equals(Paths.JOBS));

		if (elements[DEPLOYMENT_DESC] != null) {
			String[] deploymentElements = elements[DEPLOYMENT_DESC].split(" ")[0].split("\\.");
			Assert.state(deploymentElements.length == 2);
			System.arraycopy(deploymentElements, 0, deploymentDesc, 0, 2);
		}
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
	public JobDeploymentsPath setJobName(String name) {
		elements[JOB_NAME] = name;
		return this;
	}

	/**
	 * Return the module label.
	 *
	 * @return module label
	 */
	public String getModuleLabel() {
		return deploymentDesc[MODULE_LABEL];
	}

	/**
	 * Set the module label.
	 *
	 * @param label module label
	 *
	 * @return this object
	 */
	public JobDeploymentsPath setModuleLabel(String label) {
		deploymentDesc[MODULE_LABEL] = label;
		return this;
	}

	/**
	 * Return the container name.
	 *
	 * @return container name
	 */
	public String getContainer() {
		return deploymentDesc[CONTAINER];
	}

	/**
	 * Set the container name.
	 *
	 * @param container container name
	 *
	 * @return this object
	 */
	public JobDeploymentsPath setContainer(String container) {
		deploymentDesc[CONTAINER] = container;
		return this;
	}

	/**
	 * Build the path string using the field values.
	 *
	 * @return path string
	 *
	 * @throws java.lang.IllegalStateException if partial deployment info is present
	 *         (for example, if module type/label is present but container is missing)
	 * @see Paths#build
	 */
	public String build() throws IllegalStateException {
		elements[DEPLOYMENT_DESC] = (hasDeploymentInfo())
				? String.format("%s.%s", deploymentDesc[MODULE_LABEL], deploymentDesc[CONTAINER])
				: null;
		return Paths.build(stripNullElements());
	}

	/**
	 * Build the path string using the field values, including the namespace prefix.
	 *
	 * @return path string with namespace
	 *
	 * @throws java.lang.IllegalStateException if partial deployment info is present
	 *         (for example, if module type/label is present but container is missing)
	 * @see Paths#buildWithNamespace
	 */
	public String buildWithNamespace() throws IllegalStateException {
		elements[DEPLOYMENT_DESC] = (hasDeploymentInfo())
				? String.format("%s.%s", deploymentDesc[MODULE_LABEL], deploymentDesc[CONTAINER])
				: null;
		return Paths.buildWithNamespace(stripNullElements());
	}

	/**
	 * Return true if this path contains module deployment info
	 * (module type, module label, container). If false, this indicates
	 * the path only contains the stream name.
	 *
	 * @return true if this path contains module deployment info
	 * @throws java.lang.IllegalStateException if partial deployment info is present
	 *         (for example, if module type/label is present but container is missing)
	 */
	private boolean hasDeploymentInfo() {
		boolean hasValue = false;
		for (String s : deploymentDesc) {
			hasValue |= StringUtils.hasText(s);
		}
		if (!hasValue) {
			return false;
		}
		if (StringUtils.isEmpty(deploymentDesc[MODULE_LABEL])) {
			throw new IllegalStateException("Module label missing");
		}
		if (StringUtils.isEmpty(deploymentDesc[CONTAINER])) {
			throw new IllegalStateException("Container missing");
		}
		return true;
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
