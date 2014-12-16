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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hadoop.test.context.HadoopDelegatingSmartContextLoader;
import org.springframework.data.hadoop.test.context.MiniHadoopCluster;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TestFtpServer;


/**
 *
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=HadoopDelegatingSmartContextLoader.class, classes = JobCommandWithHadoopTests.EmptyConfig.class)
@MiniHadoopCluster
public class JobCommandWithHadoopTests extends AbstractJobIntegrationTest {

	private static final Log logger = LogFactory.getLog(JobCommandWithHadoopTests.class);

	@Autowired
	org.apache.hadoop.conf.Configuration configuration;

	@Test
	public void testLaunchFtpHadoopJob() throws Throwable {
		logger.info("Launch FTP->HDFS batch job");
		TestFtpServer server = new TestFtpServer("FtpHadoop");
		server.before();

		// clean up from old tests
		FileSystem fs = FileSystem.get(configuration);

		Path p1 = new Path("foo/ftpSource/ftpSource1.txt");
		fs.delete(p1, true);
		Path p2 = new Path("foo/ftpSource/ftpSource2.txt");
		fs.delete(p2, true);
		assertFalse(fs.exists(p1));
		assertFalse(fs.exists(p2));

		try {
			int port = server.getPort();
			executeJobCreate("myftphdfs", "ftphdfs --partitionResultsTimeout=120000 --port=" + port + " --fsUri=" + fs.getUri().toString());
			checkForJobInList("myftphdfs", "ftphdfs --partitionResultsTimeout=120000 --port=" + port + " --fsUri=" + fs.getUri().toString(), true);
			executeJobLaunch("myftphdfs", "{\"-remoteDirectory\":\"ftpSource\",\"hdfsDirectory\":\"foo\"}");

			Table jobExecutions = listJobExecutions();
			int n = 0;
			while (!"COMPLETED".equals(jobExecutions.getRows().get(0).getValue(5))) {
				Thread.sleep(100);
				assertTrue(n++ < 100);
				jobExecutions = listJobExecutions();
			}

			assertTrue(fs.exists(p1));
			assertTrue(fs.exists(p2));

			FSDataInputStream stream = fs.open(p1);
			byte[] out = new byte[7];
			stream.readFully(out);
			stream.close();
			assertEquals("source1", new String(out));

			stream = fs.open(p2);
			stream.readFully(out);
			stream.close();
			assertEquals("source2", new String(out));
		}
		finally {
			server.after();
		}
	}

	@Configuration
	static class EmptyConfig {
	}

}
