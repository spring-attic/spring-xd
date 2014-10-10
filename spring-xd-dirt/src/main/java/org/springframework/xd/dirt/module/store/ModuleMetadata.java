/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.module.store;

import java.io.Serializable;
import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.module.ModuleType;

/**
 * Metadata class for modules at runtime. This includes configuration
 * options, runtime deployment properties, container, and the overall
 * status of the deployment unit.
 *
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class ModuleMetadata implements Comparable<ModuleMetadata> {

	/**
	 * Unique identifier for module metadata.
	 */
	private final Id id;

	/**
	 * Options for this module configured during stream/job creation.
	 */
	private final Properties moduleOptions;

	/**
	 * Options for this module configured at deployment time.
	 */
	private final Properties deploymentProperties;

	/**
	 * Status for deployment unit this module is part of.
	 */
	private final DeploymentUnitStatus.State deploymentStatus;

	/**
	 * Construct a ModuleMetadata instance.
	 *
	 * @param id unique identifier for module metadata
	 * @param moduleOptions options for this module configured during stream/job creation
	 * @param deploymentProperties options for this module configured at deployment time
	 * @param deploymentStatus status for deployment unit this module is part of
	 */
	public ModuleMetadata(Id id, Properties moduleOptions, Properties deploymentProperties,
			DeploymentUnitStatus.State deploymentStatus) {
		this.id = id;
		this.moduleOptions = moduleOptions;
		this.deploymentProperties = deploymentProperties;
		this.deploymentStatus = deploymentStatus;
	}

	/**
	 * @see org.springframework.xd.dirt.module.store.ModuleMetadata.Id#getFullyQualifiedId()
	 */
	public String getQualifiedId() {
		return id.getFullyQualifiedId();
	}

	/**
	 * Return the name for the module instance this metadata object is tracking.
	 * The format for this string is "label.index", for example "log.1".
	 *
	 * @return name (label + index) for module
	 */
	public String getName() {
		return id.getModuleLabel() + '.' + id.getIndex();
	}

	/**
	 * @see org.springframework.xd.dirt.module.store.ModuleMetadata.Id#getModuleLabel()
	 */
	public String getLabel() {
		return id.getModuleLabel();
	}

	/**
	 * @see org.springframework.xd.dirt.module.store.ModuleMetadata.Id#getUnitName()
	 */
	public String getUnitName() {
		return id.getUnitName();
	}

	/**
	 * @see org.springframework.xd.dirt.module.store.ModuleMetadata.Id#getModuleType()
	 */
	public ModuleType getModuleType() {
		return id.getModuleType();
	}

	/**
	 * @see org.springframework.xd.dirt.module.store.ModuleMetadata.Id#getContainerId()
	 */
	public String getContainerId() {
		return id.getContainerId();
	}

	/**
	 * Return the module options configured during stream/job creation.
	 *
	 * @return module options set at creation
	 */
	public Properties getModuleOptions() {
		return moduleOptions;
	}

	/**
	 * Return the module options configured at deployment time.
	 *
	 * @return module options set at deployment
	 */
	public Properties getDeploymentProperties() {
		return deploymentProperties;
	}

	/**
	 * Return the state of the deployment unit this module instance belongs to.
	 *
	 * @return deployment unit (stream or job) for this module instance
	 */
	public DeploymentUnitStatus.State getDeploymentStatus() {
		return deploymentStatus;
	}

	@Override
	public int compareTo(ModuleMetadata other) {
		return this.getQualifiedId().compareTo(other.getQualifiedId());
	}


	/**
	 * Identity object for instances of {@link ModuleMetadata}.
	 */
	@SuppressWarnings("serial")
	public static class Id implements Serializable, Comparable<Id> {

		/**
		 * Container id of the container hosting the module instance.
		 */
		private final String containerId;

		/**
		 * Deployment unit name (stream or job) for the module instance
		 * this metadata object is tracking.
		 */
		private final String unitName;

		/**
		 * Module type for the module instance this metadata object is tracking
		 */
		private final ModuleType moduleType;

		/**
		 * Label for the module instance this metadata object is tracking.
		 *
		 * @see org.springframework.xd.module.ModuleDescriptor#getModuleLabel()
		 */
		private final String moduleLabel;

		/**
		 * Position in stream/job definition relative to other modules in the
		 * deployment unit.
		 */
		private final int index;

		/**
		 * Construct a {@link org.springframework.xd.dirt.module.store.ModuleMetadata} id.
		 *
		 * @param containerId id of container the module is deployed to
		 * @param unitName name of deployment unit the module instance belongs to
		 * @param moduleType type of module
		 * @param moduleLabel module label
		 * @param index index of module in deployment unit
		 */
		public Id(String containerId, String unitName, ModuleType moduleType, String moduleLabel, int index) {
			Assert.hasText(containerId);
			Assert.hasText(unitName);
			Assert.notNull(moduleType);
			Assert.hasText(moduleLabel);

			this.containerId = containerId;
			this.unitName = unitName;
			this.moduleType = moduleType;
			this.moduleLabel = moduleLabel;
			this.index = index;
		}

		/**
		 * Construct a {@link org.springframework.xd.dirt.module.store.ModuleMetadata} id.
		 *
		 * @param containerId id of container the module is deployed to
		 * @param fullyQualifiedId "fully qualified" id string for module instance
		 *
		 * @see #getFullyQualifiedId
		 */
		public Id(String containerId, String fullyQualifiedId) {
			Assert.hasText(containerId);
			Assert.hasText(fullyQualifiedId);

			this.containerId = containerId;
			String[] fields = fullyQualifiedId.split("\\.");
			this.unitName = fields[0];
			this.moduleType = ModuleType.valueOf(fields[1]);
			this.moduleLabel = fields[2];
			this.index = Integer.valueOf(fields[3]);
		}

		/**
		 * Return the id for the container the module instance is deployed to.
		 *
		 * @return container id hosting the module instance
		 */
		public String getContainerId() {
			return containerId;
		}

		/**
		 * Return the deployment unit name (stream or job) for the module instance
		 * this metadata object is tracking.
		 *
		 * @return deployment unit name
		 */
		public String getUnitName() {
			return unitName;
		}

		/**
		 * Return the module type for the module instance this metadata object is tracking.
		 *
		 * @return module type
		 */
		public ModuleType getModuleType() {
			return moduleType;
		}

		/**
		 * Return the label for the module instance this metadata object is tracking.
		 *
		 * @return label for module
		 */
		public String getModuleLabel() {
			return moduleLabel;
		}

		/**
		 * Return the position in stream/job definition relative to other modules
		 * in the deployment unit.
		 *
		 * @return index for module
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Return the "fully qualified" id string for the module instance
		 * this metadata object is tracking. The format for this string is
		 * "deploymentUnitName.type.label.index", for example "ticktock.sink.log.1".
		 *
		 * @return fully qualified id string
		 */
		public String getFullyQualifiedId() {
			return String.format("%s.%s.%s.%d", getUnitName(), getModuleType(),
					getModuleLabel(), getIndex());
		}

		@Override
		public int compareTo(Id that) {
			int c = this.containerId.compareTo(that.containerId);
			if (c == 0) {
				c = this.unitName.compareTo(that.unitName);
			}
			if (c == 0) {
				c = this.moduleType.compareTo(that.moduleType);
			}
			if (c == 0) {
				c = this.moduleLabel.compareTo(that.moduleLabel);
			}
			if (c == 0) {
				int x = this.index;
				int y = that.index;
				c = (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
			return c;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Id that = (Id) o;

			return this.index == that.index &&
					this.containerId.equals(that.containerId) &&
					this.moduleLabel.equals(that.moduleLabel) &&
					this.moduleType.equals(that.moduleType) &&
					this.unitName.equals(that.unitName);
		}

		@Override
		public int hashCode() {
			int result = containerId != null ? containerId.hashCode() : 0;
			result = 31 * result + (unitName != null ? unitName.hashCode() : 0);
			result = 31 * result + (moduleType != null ? moduleType.hashCode() : 0);
			result = 31 * result + (moduleLabel != null ? moduleLabel.hashCode() : 0);
			result = 31 * result + index;
			return result;
		}

		@Override
		public String toString() {
			return "Id{" +
					"containerId='" + containerId + '\'' +
					", unitName='" + unitName + '\'' +
					", moduleType=" + moduleType +
					", moduleLabel='" + moduleLabel + '\'' +
					", index=" + index +
					'}';
		}

	}

}
