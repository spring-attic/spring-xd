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
import org.springframework.xd.dirt.job.BatchJobAlreadyExistsException;

/**
 * Implementation of ListableJobLocator used by {@link DistributedJobService}
 * 
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */
public class BatchJobLocator implements ListableJobLocator {

	private static final String GET_ALL_JOB_NAMES = "SELECT JOB_NAME FROM JOB_REGISTRY_NAMES";

	private static final String JOB_INCREMENTABLE = "SELECT IS_INCREMENTABLE FROM JOB_REGISTRY_INCREMENTABLES WHERE JOB_NAME = ?";

	private static final String ADD_JOB_NAME = "INSERT INTO JOB_REGISTRY_NAMES(JOB_NAME) VALUES(?)";

	private static final String ADD_JOB_INCREMENTABLE = "INSERT INTO JOB_REGISTRY_INCREMENTABLES(JOB_NAME, IS_INCREMENTABLE) VALUES(?, ?)";

	private static final String UPDATE_JOB_INCREMENTABLE = "UPDATE JOB_REGISTRY_INCREMENTABLES SET IS_INCREMENTABLE = ? WHERE JOB_NAME = ?";

	private static final String DELETE_JOB_NAME = "DELETE FROM JOB_REGISTRY_NAMES WHERE JOB_NAME = ?";

	private static final String DELETE_JOB_INCREMENTABLE = "DELETE FROM JOB_REGISTRY_INCREMENTABLES WHERE JOB_NAME = ?";

	private static final String DELETE_ALL_JOB_INCREMENTABLE = "DELETE FROM JOB_REGISTRY_INCREMENTABLES";

	private static final String DELETE_ALL_JOB_NAMES = "DELETE FROM JOB_REGISTRY_NAMES";

	private JdbcOperations jdbcTemplate;

	@Override
	public Collection<String> getJobNames() {
		return jdbcTemplate.queryForList(GET_ALL_JOB_NAMES, String.class);
	}

	@Override
	public Job getJob(String name) throws NoSuchJobException {
		if (!getJobNames().contains(name)) {
			throw new NoSuchJobException(name);
		}
		// TODO need to create the real job here
		SimpleJob simpleJob = new SimpleJob(name);
		return simpleJob;
	}

	/**
	 * Store the job name & isIncrementable flag for the job when there is a registry update
	 * 
	 * @param name the job name to add
	 * @param isIncrementable
	 */
	protected void addJob(String name, boolean isIncrementable) {
		// String batchJobName = name + JobPlugin.JOB_NAME_DELIMITER + JobPlugin.JOB_BEAN_ID;
		Collection<String> jobNames = this.getJobNames();
		if (!jobNames.contains(name)) {
			jdbcTemplate.update(ADD_JOB_NAME, name);
			List<Boolean> incrementables = getIncrementable(name);
			if (incrementables.isEmpty()) {
				jdbcTemplate.update(ADD_JOB_INCREMENTABLE, name, isIncrementable);
			} // valueList is always single row
			else if (incrementables.get(0).booleanValue() != isIncrementable) {
				jdbcTemplate.update(UPDATE_JOB_INCREMENTABLE, isIncrementable, name);
			}
		}
		else {
			throw new BatchJobAlreadyExistsException(name);
		}
	}

	protected void delteJobName(String name) {
		jdbcTemplate.update(DELETE_JOB_NAME, name);
		jdbcTemplate.update(DELETE_JOB_INCREMENTABLE, name);
	}

	protected void deleteAll() {
		jdbcTemplate.update(DELETE_ALL_JOB_NAMES);
		jdbcTemplate.update(DELETE_ALL_JOB_INCREMENTABLE);
	}

	public Boolean isIncrementable(String name) {
		return jdbcTemplate.queryForObject(JOB_INCREMENTABLE, Boolean.class, name);
	}

	private List<Boolean> getIncrementable(String name) {
		return jdbcTemplate.query(JOB_INCREMENTABLE, new SingleColumnRowMapper<Boolean>(Boolean.class), name);
	}

	public JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
