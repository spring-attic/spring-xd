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

package org.springframework.xd.rest.client.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;

/**
 * Implementation of the Job-related part of the API.
 * 
 * @author Glenn Renfro
 */
public class JobTemplate extends AbstractTemplate implements JobOperations {

	JobTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public JobDefinitionResource createJob(String name,String definition, Boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", deploy.toString());
		JobDefinitionResource job = restTemplate.postForObject(resources.get("jobs"), values,
				JobDefinitionResource.class);
		return job;
}
	
	@Override
	public void destroyJob(String name) {
		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deployJob(String name) {

		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "true");
		restTemplate.put(uriTemplate, values, name);
	}

	@Override
	public void undeployJob(String name) {
		String uriTemplate = resources.get("jobs").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "false");
		restTemplate.put(uriTemplate, values, name);

	}

	@Override
	public List<JobDefinitionResource> list() {
		final JobDefinitionResource[] jobs = restTemplate.getForObject(resources.get("jobs"), JobDefinitionResource[].class);
		return Arrays.asList(jobs);
	}

	@Override
	public String toString() {
		return "JobTemplate [restTemplate=" + restTemplate + ", resources="
				+ resources +"]";
	}



}
