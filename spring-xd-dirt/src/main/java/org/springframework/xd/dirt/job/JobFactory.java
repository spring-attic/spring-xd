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

package org.springframework.xd.dirt.job;

import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.DeploymentPropertiesUtility;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Factory class that builds {@link Job} using {@link ModuleDescriptor} and deployment properties.
 *
 * @author Ilayaperumal Gopinathan
 */
public class JobFactory {

	private final XDStreamParser parser;

	/**
	 * JobFactory to create {@link Job} domain model.
	 * @param jobDefinitionRepository
	 * @param moduleDefinitionRepository
	 * @param moduleOptionsMetadataResolver
	 */
	public JobFactory(JobDefinitionRepository jobDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.parser = new XDStreamParser(jobDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
	}

	/**
	 * Construct {@link Job} using job module descriptor and deployment properties.
	 *
	 * @param name the name of the Job
	 * @param properties the deployment properties
	 * @return Job the runtime domain model
	 */
	public Job createJob(String name, Map<String, String> properties) {
		Assert.hasText(name, "Job name is required");
		Assert.notNull(properties, "Job properties are required");

		String definition = properties.get("definition");
		Assert.hasText(definition, "Job properties requires a 'definition' property");

		List<ModuleDescriptor> descriptors = parser.parse(name, definition, ParsingContext.job);
		Assert.isTrue(descriptors.size() == 1);
		return new Job.Builder()
				.setName(name)
				.setDeploymentProperties(
						DeploymentPropertiesUtility.parseDeploymentProperties(properties.get("deploymentProperties")))
				.setModuleDescriptor(descriptors.get(0)).build();
	}

}
