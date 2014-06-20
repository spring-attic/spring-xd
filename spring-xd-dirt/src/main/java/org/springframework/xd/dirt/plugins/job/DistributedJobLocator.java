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

	private static final String GET_ALL_JOB_NAMES = "SELECT JOB_NAME FROM XD_JOB_REGISTRY";

	private static final String GET_ALL_RESTARTABLE_JOBS = "SELECT JOB_NAME FROM XD_JOB_REGISTRY WHERE IS_RESTARTABLE='true'";

	private static final String JOB_INCREMENTABLE = "SELECT IS_INCREMENTABLE FROM XD_JOB_REGISTRY WHERE JOB_NAME = ?";

	private static final String JOB_RESTARTABLE = "SELECT IS_RESTARTABLE FROM XD_JOB_REGISTRY WHERE JOB_NAME = ?";

	private static final String GET_STEP_NAMES = "SELECT STEP_NAME FROM XD_JOB_REGISTRY_STEP_NAMES WHERE JOB_NAME = ?";

	private static final String ADD_JOB_REGISTRY = "INSERT INTO XD_JOB_REGISTRY(IS_INCREMENTABLE, IS_RESTARTABLE, JOB_NAME) VALUES(?, ?, ?)";

	private static final String ADD_STEP_NAME = "INSERT INTO XD_JOB_REGISTRY_STEP_NAMES(JOB_NAME, STEP_NAME) VALUES(?, ?)";

	private static final String UPDATE_JOB_REGISTRY = "UPDATE XD_JOB_REGISTRY SET IS_INCREMENTABLE = ?, IS_RESTARTABLE = ? WHERE JOB_NAME = ?";

	private static final String DELETE_JOB_REGISTRY = "DELETE FROM XD_JOB_REGISTRY WHERE JOB_NAME = ?";

	private static final String DELETE_STEP_NAMES = "DELETE FROM XD_JOB_REGISTRY_STEP_NAMES WHERE JOB_NAME = ?";

	private static final String DELETE_ALL_JOB_REGISTRY = "DELETE FROM XD_JOB_REGISTRY";

	private static final String DELETE_ALL_STEP_NAMES = "DELETE FROM XD_JOB_REGISTRY_STEP_NAMES";


	private JdbcOperations jdbcTemplate;

	/**
	 * Get all the deployed job names.
	 */
	@Override
	public Collection<String> getJobNames() {
		return jdbcTemplate.queryForList(GET_ALL_JOB_NAMES, String.class);
	}

	/**
	 * Get all the deployed job names that can be restarted.
	 *
	 * @return collection of job names.
	 */
	public Collection<String> getAllRestartableJobs() {
		return jdbcTemplate.queryForList(GET_ALL_RESTARTABLE_JOBS, String.class);
	}

	/**
	 * Get simple batch job representation for the given job name.
	 *
	 * @param name the job name
	 * @return Job a simple batch job consisting of its step names.
	 */
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

	/**
	 * Get all the steps' names of a given job name.
	 *
	 * @param jobName
	 * @return the list of step names.
	 */
	public List<String> getJobStepNames(String jobName) {
		return jdbcTemplate.query(GET_STEP_NAMES, new SingleColumnRowMapper<String>(String.class), jobName);
	}

	/**
	 * Store the job name , job parameterer incrementable and job restartable
	 * flags for the job when there is a registry update
	 * 
	 * @param name the job name to add
	 * @param incrementable flag to specify if the job parameter is incrementable
	 * @param restartable flag to specify if the job is restartable
	 */
	protected void addJob(String name, boolean incrementable, boolean restartable) {
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
			addJobName(name, incrementable, restartable);
		}
		else {
			updateJobName(name, incrementable, restartable);
		}
	}

	/**
	 * Add a new job entry into the XD_JOB_REGISTRY table.
	 *
	 * @param name the name of the job
	 * @param incrementable flag to specify if the job parameter can be incremented
	 * @param restartable flag to specify if the job can be restarted upon failure/stoppage.
	 */
	private void addJobName(String name, boolean incrementable, boolean restartable) {
		jdbcTemplate.update(ADD_JOB_REGISTRY, incrementable, restartable, name);
	}

	/**
	 * Update an existing job entry at the XD_JOB_REGISTRY table.
	 *
	 * @param name the name of the job
	 * @param incrementable flag to specify if the job parameter can be incremented
	 * @param restartable flag to specify if the job can be restarted upon failure/stoppage.
	 */
	private void updateJobName(String name, boolean incrementable, boolean restartable) {
		jdbcTemplate.update(UPDATE_JOB_REGISTRY, incrementable, restartable, name);
	}

	/**
	 * Add the collection of step names into XD_JOB_REGISTRY_STEP_NAMES for a given job.
	 *
	 * @param jobName the job name
	 * @param stepNames the collection of step names associated with this job
	 */
	protected void addStepNames(String jobName, Collection<String> stepNames) {
		for (String stepName : stepNames) {
			jdbcTemplate.update(ADD_STEP_NAME, jobName, stepName);
		}
	}

	/**
	 * Delete the job entry from the registry table.
	 * This will delete job registry and the associated step names entries
	 * for the given job name.
	 *
	 * @param jobName the job name.
	 */
	protected void deleteJobRegistry(String jobName) {
		jdbcTemplate.update(DELETE_JOB_REGISTRY, jobName);
		jdbcTemplate.update(DELETE_STEP_NAMES, jobName);
	}

	/**
	 * Delete all the registry and step name entries.
	 */
	protected void deleteAll() {
		jdbcTemplate.update(DELETE_ALL_JOB_REGISTRY);
		jdbcTemplate.update(DELETE_ALL_STEP_NAMES);
	}

	/**
	 * @param jobName the job name
	 * @return Boolean flag to specify if the job parameter can be incremented for the given job name.
	 */
	public Boolean isIncrementable(String jobName) {
		return jdbcTemplate.queryForObject(JOB_INCREMENTABLE, Boolean.class, jobName);
	}

	/**
	 * @param jobName the job name
	 * @return Boolean flag to specify if the job can be restarted.
	 */
	public Boolean isRestartable(String jobName) {
		return jdbcTemplate.queryForObject(JOB_RESTARTABLE, Boolean.class, jobName);
	}

	public JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
