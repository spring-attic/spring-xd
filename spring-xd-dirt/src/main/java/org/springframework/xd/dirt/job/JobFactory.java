/*
 * Copyright 2014-2015 the original author or authors.
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
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.job.dsl.ComposedJobUtil;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Factory class that builds {@link Job} instances.
 *
 * @author Ilayaperumal Gopinathan
 */
public class JobFactory {

	/**
	 * DSL parser.
	 */
	private final XDStreamParser parser;

	/**
	 * Construct a JobFactory to create {@link Job} domain model instances.
	 *
	 * @param jobDefinitionRepository        repository for job definitions
	 * @param moduleRegistry			     registry for module definitions
	 * @param moduleOptionsMetadataResolver  resolver for module options metadata
	 */
	public JobFactory(JobDefinitionRepository jobDefinitionRepository,
			ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.parser = new XDStreamParser(jobDefinitionRepository, moduleRegistry,
				moduleOptionsMetadataResolver);
	}

	/**
	 * Create a new instance of {@link Job} for the given name and
	 * properties. The properties should at minimum contain the following
	 * entries:
	 * <table>
	 *   <tr>
	 *     <th>definition</th>
	 *     <td>DSL definition for job</td>
	 *   </tr>
	 *   <tr>
	 *     <th>deploymentProperties</th>
	 *     <td>Deployment properties for job</td>
	 *   </tr>
	 * </table>
	 *
	 * @param name        job name
	 * @param properties  properties for job
	 * @return new {@code Job} domain model instance
	 */
	public Job createJob(String name, Map<String, String> properties) {
		Assert.hasText(name, "Job name is required");
		Assert.notNull(properties, "Job properties are required");

		String definition = properties.get("definition");
		Assert.hasText(definition, "Job properties requires a 'definition' property");
		if (ComposedJobUtil.isComposedJobDefinition(definition)){
			definition = ComposedJobUtil.getComposedJobModuleName(name) + 
				ComposedJobUtil.getDefinitionParameters(definition);
		}

		List<ModuleDescriptor> descriptors = parser.parse(name, definition, ParsingContext.job);
		Assert.isTrue(descriptors.size() == 1);
		return new Job.Builder()
				.setName(name)
				.setDeploymentProperties(
						DeploymentPropertiesFormat.parseDeploymentProperties(properties.get("deploymentProperties")))
				.setModuleDescriptor(descriptors.get(0)).build();
	}

}
