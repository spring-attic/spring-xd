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
 * Builder object for paths under {@link Paths#DEPLOYMENTS} for module deployments. {@code ModuleDeploymentsPath}
 * can be used to take a full path and split it into its elements, for example:
 * <p>
 * <code>
 * ModuleDeploymentsPath deploymentsPath =
 *     new ModuleDeploymentsPath("/xd/deployments/modules/4dbd28e2-880d-4774/my-stream.source.http-0");
 * assertEquals("my-stream", deploymentsPath.getStreamName());
 * </code>
 * </p>
 * It can also be used to build a path, for example:
 * <p>
 * <code>
 * ModuleDeploymentsPath deploymentsPath = new ModuleDeploymentsPath().setStreamName("my-stream").setContainer(...)...;
 * assertEquals("/xd/deployments/modules/4dbd28e2-880d-4774/my-stream.source.http-0", deploymentsPath.build());
 * </code>
 * </p>
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDeploymentsPath {

	/**
	 * Index for {@link Paths#DEPLOYMENTS} in {@link #elements} array.
	 */
	private static final int DEPLOYMENTS = 0;

	/**
	 * Index for {@code modules} in {@link #elements} array.
	 */
	private static final int MODULES = 1;

	/**
	 * Index for {@link Paths#ALLOCATED} node in {@link #elements} array.
	 */
	private static final int ALLOCATED = 2;

	/**
	 * Index for container name in {@link #elements} array.
	 */
	private static final int CONTAINER = 3;

	/**
	 * Index for dot delimited module deployment description in {@link #elements} array.
	 */
	private static final int DEPLOYMENT_DESC = 4;

	/**
	 * Index for deployment unit(Stream/Job) name in dot delimited deployment description.
	 */
	private static final int DEPLOYMENT_UNIT_NAME = 0;

	/**
	 * Index for module type in dot delimited deployment description.
	 */
	private static final int MODULE_TYPE = 1;

	/**
	 * Index for module label in dot delimited deployment description.
	 */
	private static final int MODULE_LABEL = 2;

	/**
	 * Index for module sequence in dot delimited deployment description.
	 */
	private static final int MODULE_SEQUENCE = 3;

	/**
	 * Array of path elements.
	 */
	private final String[] elements = new String[5];

	/**
	 * Array of module deployment description elements.
	 */
	private final String[] deploymentDesc = new String[4];

	/**
	 * Construct a {@code DeploymentsPath}. Use of this constructor means that a path will be created via
	 * {@link #build()}.
	 */
	public ModuleDeploymentsPath() {
		elements[DEPLOYMENTS] = Paths.DEPLOYMENTS;
		elements[MODULES] = Paths.MODULES;
		elements[ALLOCATED] = Paths.ALLOCATED;
	}

	/**
	 * Construct a {@code DeploymentsPath}. Use of this constructor means that an existing path will be provided and
	 * this object will be used to extract the individual elements of the path. Both full paths (including and excluding
	 * the {@link Paths#XD_NAMESPACE XD namespace prefix}) are supported.
	 *
	 * @param path stream path
	 */
	public ModuleDeploymentsPath(String path) {
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

		System.arraycopy(pathElements, offset, elements, 0, elements.length);

		Assert.noNullElements(elements);
		Assert.state(elements[DEPLOYMENTS].equals(Paths.DEPLOYMENTS));

		String[] deploymentElements = elements[DEPLOYMENT_DESC].split(" ")[0].split("\\.");

		Assert.state(deploymentElements.length == 4);

		System.arraycopy(deploymentElements, 0, deploymentDesc, 0, deploymentDesc.length);
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
	public ModuleDeploymentsPath setContainer(String container) {
		elements[CONTAINER] = container;
		return this;
	}

	/**
	 * Return the deployment unit (Stream/Job) name.
	 *
	 * @return deployment unit name
	 */
	public String getDeploymentUnitName() {
		return deploymentDesc[DEPLOYMENT_UNIT_NAME];
	}

	/**
	 * Set the deployment unit name.
	 *
	 * @param deploymentUnitName deployment unit name
	 *
	 * @return this object
	 */
	public ModuleDeploymentsPath setDeploymentUnitName(String deploymentUnitName) {
		deploymentDesc[DEPLOYMENT_UNIT_NAME] = deploymentUnitName;
		return this;
	}

	/**
	 * Return the module type.
	 *
	 * @return module type
	 */
	public String getModuleType() {
		return deploymentDesc[MODULE_TYPE];
	}

	/**
	 * Set the module type.
	 *
	 * @param moduleType module type
	 *
	 * @return this object
	 */
	public ModuleDeploymentsPath setModuleType(String moduleType) {
		deploymentDesc[MODULE_TYPE] = moduleType;
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
	 * @param moduleLabel module label
	 *
	 * @return this object
	 */
	public ModuleDeploymentsPath setModuleLabel(String moduleLabel) {
		deploymentDesc[MODULE_LABEL] = moduleLabel;
		return this;
	}

	/**
	 * Return the module sequence as string.
	 *
	 * @return module sequence
	 */
	public String getModuleSequenceAsString() {
		return deploymentDesc[MODULE_SEQUENCE];
	}

	/**
	 * Return the module sequence.
	 *
	 * @return module sequence
	 */
	public int getModuleSequence() {
		return Integer.valueOf(deploymentDesc[MODULE_SEQUENCE]);
	}

	/**
	 * Set the module sequence.
	 *
	 * @param moduleSequence module sequence
	 *
	 * @return this object
	 */
	public ModuleDeploymentsPath setModuleSequence(String moduleSequence) {
		deploymentDesc[MODULE_SEQUENCE] = moduleSequence;
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
		elements[DEPLOYMENT_DESC] = String.format("%s.%s.%s.%s",
				deploymentDesc[DEPLOYMENT_UNIT_NAME], deploymentDesc[MODULE_TYPE], deploymentDesc[MODULE_LABEL],
				deploymentDesc[MODULE_SEQUENCE]);
		return Paths.build(elements);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return build();
	}

}
