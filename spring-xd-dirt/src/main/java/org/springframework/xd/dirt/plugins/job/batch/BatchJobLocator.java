/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job.batch;

import java.util.Collection;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.jdbc.core.JdbcOperations;


/**
 * Implementation of ListableJobLocator used by {@DistributedJobService}
 * 
 * @author Ilayaperumal Gopinathan
 */
public class BatchJobLocator implements ListableJobLocator {

	private static final String ALL_JOBS_NAMES = "SELECT JOB_NAME FROM REGISTRY_NAMES";

	private static final String JOB_INCREMENTABLE = "SELECT IS_INCREMENTABLE FROM REGISTRY_NAMES WHERE JOB_NAME = ?";

	private static final String ADD_JOB = "INSERT INTO REGISTRY_NAMES(JOB_NAME, IS_INCREMENTABLE) VALUES(?, ?)";

	private static final String DELETE_JOB = "DELETE FROM REGISTRY_NAMES WHERE JOB_NAME = ?";

	private static final String DELETE_ALL_JOBS = "DELETE FROM REGISTRY_NAMES";


	private JdbcOperations jdbcTemplate;

	@Override
	public Collection<String> getJobNames() {
		return jdbcTemplate.queryForList(ALL_JOBS_NAMES, String.class);
	}

	@Override
	public Job getJob(String name) throws NoSuchJobException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("JobLocator.getJob(String) is not implemented yet.");
	}

	/**
	 * Store the job name & isIncrementable flag for the job when there is a registry update
	 * 
	 * @param name the job name to add
	 * @param isIncrementable
	 */
	public void addJob(String name, boolean isIncrementable) {
		// String batchJobName = name + JobPlugin.JOB_NAME_DELIMITER + JobPlugin.JOB_BEAN_ID;
		Collection<String> jobNames = this.getJobNames();
		if (!jobNames.contains(name)) {
			jdbcTemplate.update(ADD_JOB, name, isIncrementable);
		}
		else {
			throw new BatchJobAlreadyExistsException(name);
		}
	}

	public void deleteJob(String name) {
		jdbcTemplate.update(DELETE_JOB, name);
	}

	public void deleteAll() {
		jdbcTemplate.update(DELETE_ALL_JOBS);
	}

	public Boolean isIncrementable(String name) {
		return jdbcTemplate.queryForObject(JOB_INCREMENTABLE, Boolean.class, name);
	}

	public JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
