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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionProgressInfoResource;

/**
 * Implementation of the Job-related part of the API.
 * 
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
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

		JobDefinitionResource job = restTemplate.postForObject(resources.get("jobs"), values,
				JobDefinitionResource.class);
		return job;
	}

	@Override
	public void destroy(String name) {
		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deploy(String name) {

		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "true");

		restTemplate.put(uriTemplate, values, name);
	}

	@Override
	public void launchJob(String name, String jobParameters) {
		String uriTemplate = resources.get("jobs").toString() + "/{name}/launch";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("jobParameters", jobParameters);
		restTemplate.put(uriTemplate, values, Collections.singletonMap("name", name));
	}

	@Override
	public void stopAllJobExecutions() {
		String uriTemplate = resources.get("batch/executions").toString();
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("stop", "true");
		restTemplate.put(uriTemplate, values);
	}

	@Override
	public void stopJobExecution(long executionId) {
		String uriTemplate = resources.get("batch/executions").toString() + "/{executionId}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("executionId", executionId);
		uriTemplate = uriTemplate + "?stop=true";
		restTemplate.put(uriTemplate, values, executionId);
	}

	@Override
	public void undeploy(String name) {
		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "false");
		restTemplate.put(uriTemplate, values, name);
	}

	@Override
	public JobDefinitionResource.Page list() {
		String uriTemplate = resources.get("jobs").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000&deployments=true";
		return restTemplate.getForObject(uriTemplate, JobDefinitionResource.Page.class);
	}

	@Override
	public void undeployAll() {
		String uriTemplate = resources.get("jobs").toString() + DEPLOYMENTS_URI;
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "false");
		restTemplate.put(uriTemplate, values);
	}

	@Override
	public void deployAll() {
		String uriTemplate = resources.get("jobs").toString() + DEPLOYMENTS_URI;
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "true");
		restTemplate.put(uriTemplate, values);
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(resources.get("jobs"));
	}

	@Override
	public String toString() {
		return "JobTemplate [restTemplate=" + restTemplate + ", resources=" + resources + "]";
	}

	@Override
	public List<JobExecutionInfoResource> listJobExecutions() {
		String uriTemplate = resources.get("batch/executions").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000";
		JobExecutionInfoResource[] jobExecutionInfoResources = restTemplate.getForObject(uriTemplate,
				JobExecutionInfoResource[].class);
		return Arrays.asList(jobExecutionInfoResources);
	}

	@Override
	public JobExecutionInfoResource displayJobExecution(long jobExecutionId) {
		String uriTemplate = resources.get("batch/executions").toString() + "/{jobExecutionId}";
		return restTemplate.getForObject(uriTemplate, JobExecutionInfoResource.class, jobExecutionId);
	}

	@Override
	public List<StepExecutionInfoResource> listStepExecutions(long jobExecutionId) {
		String uriTemplate = resources.get("batch/executions").toString() + "/{jobExecutionId}/steps";
		StepExecutionInfoResource[] stepExecutionInfoResources = restTemplate.getForObject(uriTemplate,
				StepExecutionInfoResource[].class, jobExecutionId);
		return Arrays.asList(stepExecutionInfoResources);
	}

	@Override
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId) {
		String uriTemplate = resources.get("batch/executions").toString()
				+ "/{jobExecutionId}/steps/{stepExecutionId}/progress";
		StepExecutionProgressInfoResource progressInfoResource = restTemplate.getForObject(uriTemplate,
				StepExecutionProgressInfoResource.class, jobExecutionId, stepExecutionId);
		return progressInfoResource;
	}
}
