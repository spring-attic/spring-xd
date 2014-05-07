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

package org.springframework.xd.dirt.plugins.job;

import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * Implementation of ListableJobLocator used by {@link DistributedJobService}. This class also provides support methods
 * to work with batch jobs in a distributed environment.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */
public class DistributedJobLocator implements ListableJobLocator {

	private static final String GET_ALL_JOB_NAMES = "SELECT JOB_NAME FROM JOB_REGISTRY_NAMES";

	private static final String JOB_INCREMENTABLE = "SELECT IS_INCREMENTABLE FROM JOB_REGISTRY_INCREMENTABLES WHERE JOB_NAME = ?";

	private static final String GET_STEP_NAMES = "SELECT STEP_NAME FROM JOB_REGISTRY_STEP_NAMES WHERE JOB_NAME = ?";

	private static final String ADD_JOB_NAME = "INSERT INTO JOB_REGISTRY_NAMES(JOB_NAME) VALUES(?)";

	private static final String ADD_JOB_INCREMENTABLE = "INSERT INTO JOB_REGISTRY_INCREMENTABLES(JOB_NAME, IS_INCREMENTABLE) VALUES(?, ?)";

	private static final String ADD_STEP_NAME = "INSERT INTO JOB_REGISTRY_STEP_NAMES(JOB_NAME, STEP_NAME) VALUES(?, ?)";

	private static final String UPDATE_JOB_INCREMENTABLE = "UPDATE JOB_REGISTRY_INCREMENTABLES SET IS_INCREMENTABLE = ? WHERE JOB_NAME = ?";

	private static final String DELETE_JOB_NAME = "DELETE FROM JOB_REGISTRY_NAMES WHERE JOB_NAME = ?";

	private static final String DELETE_JOB_INCREMENTABLE = "DELETE FROM JOB_REGISTRY_INCREMENTABLES WHERE JOB_NAME = ?";

	private static final String DELETE_STEP_NAMES = "DELETE FROM JOB_REGISTRY_STEP_NAMES WHERE JOB_NAME = ?";

	private static final String DELETE_ALL_JOB_INCREMENTABLE = "DELETE FROM JOB_REGISTRY_INCREMENTABLES";

	private static final String DELETE_ALL_JOB_NAMES = "DELETE FROM JOB_REGISTRY_NAMES";

	private static final String DELETE_ALL_STEP_NAMES = "DELETE FROM JOB_REGISTRY_STEP_NAMES";


	private JdbcOperations jdbcTemplate;

	@Override
	public Collection<String> getJobNames() {
		return jdbcTemplate.queryForList(GET_ALL_JOB_NAMES, String.class);
	}

	@Override
	public Job getJob(final String name) throws NoSuchJobException {
		if (!getJobNames().contains(name)) {
			throw new NoSuchJobException(name);
		}
		// Return a simple job that currently supports
		// - Get job name
		// - Get step names of the given job
		SimpleJob job = new SimpleJob(name) {

			@Override
			public Collection<String> getStepNames() {
				return getJobStepNames(name);
			}
		};
		return job;
	}

	public List<String> getJobStepNames(String jobName) {
		return jdbcTemplate.query(GET_STEP_NAMES, new SingleColumnRowMapper<String>(String.class), jobName);
	}

	/**
	 * Store the job name & isIncrementable flag for the job when there is a registry update
	 * 
	 * @param name the job name to add
	 * @param isIncrementable
	 */
	protected void addJob(String name, boolean isIncrementable) {
		Collection<String> jobNames = this.getJobNames();
		// XD admin server will prevent any REST client requests create a job definition with an existing name.
		// The container could handle the deployment of mutilple job modules with the same name in the following
		// scenarios:
		// 1) There could be multiple deployments of job modules (of the *same* job definition) inside a single
		// or multiple containers.
		// 2) The container that had the job module deployed crashes(without graceful shutdown), thereby
		// leaving the batch job entry in {@link DistributedJobLocator}. Had the container been shutdown gracefully,
		// then the destroy life-cycle method on batch job will take care deleting the job name entry from
		// {@link DistributedJobLocator}
		// Since, it is the same job with the given name, we can skip the update into {@link DistributedJobLocator}
		if (!jobNames.contains(name)) {
			updateJobName(name, isIncrementable);
		}
	}

	private void updateJobName(String name, boolean isIncrementable) {
		jdbcTemplate.update(ADD_JOB_NAME, name);
		List<Boolean> incrementables = getIncrementable(name);
		if (incrementables.isEmpty()) {
			jdbcTemplate.update(ADD_JOB_INCREMENTABLE, name, isIncrementable);
		} // valueList is always single row
		else if (incrementables.get(0).booleanValue() != isIncrementable) {
			jdbcTemplate.update(UPDATE_JOB_INCREMENTABLE, isIncrementable, name);
		}
	}

	protected void addStepNames(String jobName, Collection<String> stepNames) {
		for (String stepName : stepNames) {
			jdbcTemplate.update(ADD_STEP_NAME, jobName, stepName);
		}
	}

	protected void deleteJobName(String jobName) {
		jdbcTemplate.update(DELETE_JOB_NAME, jobName);
		jdbcTemplate.update(DELETE_JOB_INCREMENTABLE, jobName);
		jdbcTemplate.update(DELETE_STEP_NAMES, jobName);
	}

	protected void deleteAll() {
		jdbcTemplate.update(DELETE_ALL_JOB_NAMES);
		jdbcTemplate.update(DELETE_ALL_JOB_INCREMENTABLE);
		jdbcTemplate.update(DELETE_ALL_STEP_NAMES);
	}

	public Boolean isIncrementable(String jobName) {
		return jdbcTemplate.queryForObject(JOB_INCREMENTABLE, Boolean.class, jobName);
	}

	private List<Boolean> getIncrementable(String jobName) {
		return jdbcTemplate.query(JOB_INCREMENTABLE, new SingleColumnRowMapper<Boolean>(Boolean.class), jobName);
	}

	public JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
