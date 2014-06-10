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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.xd.module.ModuleDescriptor;

/**
 * Domain model for Job runtime module. A Job consists of a single job module.
 *
 * @author Ilayaperumal Gopinathan
 */
public class Job implements DeploymentUnit {

	/**
	 * The name of the job.
	 */
	private final String name;

	/**
	 * {@link ModuleDescriptor} for the job module.
	 */
	private final ModuleDescriptor descriptor;

	/**
	 * Deployment properties for this job.
	 */
	private final Map<String, String> deploymentProperties;

	/**
	 * Construct a Job.
	 *
	 * @param name job name
	 * @param descriptor module descriptor defined by this job
	 * @param deploymentProperties job deployment properties
	 */
	private Job(String name, ModuleDescriptor descriptor, Map<String, String> deploymentProperties) {
		this.name = name;
		this.descriptor = descriptor;
		this.deploymentProperties = deploymentProperties;
	}

	/**
	 * Return the name of this job.
	 *
	 * @return job name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Return the job module descriptor.
	 *
	 * @return the job module descriptor for this job definition
	 */
	public ModuleDescriptor getJobModuleDescriptor() {
		return descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModuleDescriptor> getModuleDescriptors() {
		return Collections.singletonList(descriptor);
	}

	/**
	 * Return the deployment properties for this job.
	 *
	 * @return job deployment properties
	 */
	@Override
	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Job{name='" + name + "'}";
	}

	/**
	 * Builder object for {@link Job} that supports fluent style configuration.
	 */
	public static class Builder {

		/**
		 * @see Job#name
		 */
		private String name;

		/**
		 * @see Job#deploymentProperties
		 */
		private Map<String, String> deploymentProperties = Collections.emptyMap();

		/**
		 * @see Job#descriptor
		 */
		private ModuleDescriptor moduleDescriptor;

		/**
		 * Set the job name.
		 *
		 * @param name job name
		 *
		 * @return this builder
		 */
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the module descriptor for this job.
		 *
		 * @param descriptor the job module descriptor
		 * @return this builder
		 */
		public Builder setModuleDescriptor(ModuleDescriptor descriptor) {
			this.moduleDescriptor = descriptor;
			return this;
		}

		/**
		 * Set the deployment properties for the job.
		 *
		 * @param deploymentProperties job deployment properties
		 *
		 * @return this builder
		 */
		public Builder setDeploymentProperties(Map<String, String> deploymentProperties) {
			this.deploymentProperties = deploymentProperties;
			return this;
		}

		/**
		 * Create a new instance of {@link Job}.
		 *
		 * @return new Job instance
		 */
		public Job build() {
			return new Job(name, moduleDescriptor, deploymentProperties);
		}

	}

}
