/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javax.sql.DataSource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.batch.hsqldb.server.HsqlServerApplication;
import org.springframework.xd.dirt.server.ParentConfiguration;
import org.springframework.xd.test.RandomConfigurationSupport;


/**
 * Tests for Batch Job Repository.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { EmbeddedDataSourceConfiguration.class, HsqlServerApplication.class,
	ParentConfiguration.class }, loader = SpringApplicationContextLoader.class)
@ActiveProfiles({ HsqlServerApplication.HSQLDBSERVER_PROFILE })
@DirtiesContext
public class JobRepoTests extends RandomConfigurationSupport {

	private static final String SIMPLE_JOB_NAME = "foobar";

	@Autowired
	private DataSource source;

	@Autowired
	private JobRepository repo;

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private DistributedJobLocator jobLocator;

	private static SimpleJob job;

	@BeforeClass
	public static void setup() {
		// use a different job repo database for this test
		System.setProperty("hsql.server.database", "jobrepotest");
		job = new SimpleJob(SIMPLE_JOB_NAME) {

			@Override
			public Collection<String> getStepNames() {
				return Arrays.asList(("step1,step2").split(","));
			}
		};
	}

	@Test
	public void checkThatRepoTablesAreCreated() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(source);
		int count = jdbcTemplate.queryForObject(
				"select count(*) from INFORMATION_SCHEMA.system_tables  WHERE TABLE_NAME LIKE 'BATCH_%'",
				Integer.class).intValue();
		assertEquals("The number of batch tables returned from hsqldb did not match.", 9, count);
	}

	@Test
	public void checkThatRegistryTablesAreCreated() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(source);
		int count = jdbcTemplate.queryForObject(
				"select count(*) from INFORMATION_SCHEMA.system_tables  WHERE TABLE_NAME LIKE 'XD_JOB_REGISTRY%'",
				Integer.class).intValue();
		assertEquals("The number of batch tables returned from hsqldb did not match.", 2, count);
	}

	@Test
	public void checkThatStepNamesAreUpdated() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(source);
		jobLocator.addStepNames(SIMPLE_JOB_NAME, job.getStepNames());
		int count = jdbcTemplate.queryForObject(
				"select count(*) from XD_JOB_REGISTRY_STEP_NAMES  WHERE JOB_NAME = ?", Integer.class, job.getName()).intValue();
		assertEquals("The number of step names returned from hsqldb did not match.", 2, count);
	}

	@Test
	public void checkThatContainerHasRepo() throws Exception {
		Job job = new SimpleJob(SIMPLE_JOB_NAME);
		try {
			launcher.run(job, new JobParameters());
		}
		catch (Exception ex) {
			// we can ignore this. Just want to create a fake job instance.
		}
		assertTrue(repo.isJobInstanceExists(SIMPLE_JOB_NAME, new JobParameters()));
		jobLocator.deleteJobRegistry(SIMPLE_JOB_NAME);
	}
}
