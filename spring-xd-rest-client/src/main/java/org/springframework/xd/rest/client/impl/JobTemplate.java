/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.rest.client.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.domain.JobDefinitionResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.JobInstanceInfoResource;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.domain.StepExecutionProgressInfoResource;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Implementation of the Job-related part of the API.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Eric Bottard
 */
public class JobTemplate extends AbstractTemplate implements JobOperations {

	JobTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public JobDefinitionResource createJob(String name, String definition, boolean deploy) {

		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("deploy", String.valueOf(deploy));
		values.add("definition", definition);

		JobDefinitionResource job = restTemplate.postForObject(resources.get("jobs/definitions").expand(), values,
				JobDefinitionResource.class);
		return job;
	}

	@Override
	public void destroy(String name) {
		String uriTemplate = resources.get("jobs/definitions").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		String uriTemplate = resources.get("jobs/deployments").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("properties", DeploymentPropertiesFormat.formatDeploymentProperties(properties));
		//TODO: Do we need JobDeploymentResource?
		restTemplate.postForObject(uriTemplate, values, Object.class, name);
	}

	@Override
	public void launchJob(String name, String jobParameters) {
		String uriTemplate = resources.get("jobs/executions").toString();
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("jobParameters", jobParameters);
		values.add("jobname", name);
		restTemplate.postForObject(uriTemplate, values, Object.class);
	}

	@Override
	public void stopAllJobExecutions() {
		String uriTemplate = resources.get("jobs/executions").toString();
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("stop", "true");
		restTemplate.put(uriTemplate, values);
	}

	@Override
	public void stopJobExecution(long executionId) {
		String uriTemplate = resources.get("jobs/executions").toString() + "/{executionId}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("executionId", executionId);
		uriTemplate = uriTemplate + "?stop=true";
		restTemplate.put(uriTemplate, values, executionId);
	}

	@Override
	public void restartJobExecution(long executionId) {
		String uriTemplate = resources.get("jobs/executions").toString() + "/{executionId}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("executionId", executionId);
		uriTemplate = uriTemplate + "?restart=true";
		restTemplate.put(uriTemplate, values, executionId);
	}

	@Override
	public void undeploy(String name) {
		String uriTemplate = resources.get("jobs/deployments").toString() + "/{name}";
		restTemplate.delete(uriTemplate, name);
	}

	@Override
	public JobDefinitionResource.Page list() {
		String uriTemplate = resources.get("jobs/definitions").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000&deployments=true";
		return restTemplate.getForObject(uriTemplate, JobDefinitionResource.Page.class);
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(resources.get("jobs/deployments").expand());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(resources.get("jobs/definitions").expand());
	}

	@Override
	public String toString() {
		return "JobTemplate [restTemplate=" + restTemplate + ", resources=" + resources + "]";
	}

	@Override
	public JobExecutionInfoResource.Page listJobExecutions() {
		String uriTemplate = resources.get("jobs/executions").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, JobExecutionInfoResource.Page.class);
	}

	@Override
	public JobExecutionInfoResource displayJobExecution(long jobExecutionId) {
		String uriTemplate = resources.get("jobs/executions").toString() + "/{jobExecutionId}";
		return restTemplate.getForObject(uriTemplate, JobExecutionInfoResource.class, jobExecutionId);
	}

	@Override
	public List<StepExecutionInfoResource> listStepExecutions(long jobExecutionId) {
		String uriTemplate = resources.get("jobs/executions").toString() + "/{jobExecutionId}/steps";
		StepExecutionInfoResource[] stepExecutionInfoResources = restTemplate.getForObject(uriTemplate,
				StepExecutionInfoResource[].class, jobExecutionId);
		return Arrays.asList(stepExecutionInfoResources);
	}

	@Override
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId) {
		String uriTemplate = resources.get("jobs/executions").toString()
				+ "/{jobExecutionId}/steps/{stepExecutionId}/progress";
		StepExecutionProgressInfoResource progressInfoResource = restTemplate.getForObject(uriTemplate,
				StepExecutionProgressInfoResource.class, jobExecutionId, stepExecutionId);
		return progressInfoResource;
	}

	@Override
	public StepExecutionInfoResource displayStepExecution(long jobExecutionId, long stepExecutionId) {
		String uriTemplate = resources.get("jobs/executions").toString()
				+ "/{jobExecutionId}/steps/{stepExecutionId}";
		final StepExecutionInfoResource stepExecutionInfoResource = restTemplate.getForObject(uriTemplate,
				StepExecutionInfoResource.class, jobExecutionId, stepExecutionId);
		return stepExecutionInfoResource;
	}

	@Override
	public JobInstanceInfoResource displayJobInstance(long instanceId) {
		String uriTemplate = resources.get("jobs/instances").toString() + "/{instanceId}";
		return restTemplate.getForObject(uriTemplate, JobInstanceInfoResource.class, instanceId);
	}
}
