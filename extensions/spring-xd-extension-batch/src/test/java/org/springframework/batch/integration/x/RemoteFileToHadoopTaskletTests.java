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

package org.springframework.batch.integration.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hadoop.test.context.HadoopDelegatingSmartContextLoader;
import org.springframework.data.hadoop.test.context.MiniHadoopCluster;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 *
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = HadoopDelegatingSmartContextLoader.class, classes = RemoteFileToHadoopTaskletTests.EmptyConfig.class)
@MiniHadoopCluster
public class RemoteFileToHadoopTaskletTests {

	private static final String tmpDir = System.getProperty("java.io.tmpdir");

	@Autowired
	org.apache.hadoop.conf.Configuration configuration;

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testWrite() throws Exception {
		File file = new File(tmpDir, "foo.txt");
		file.delete();

		ByteArrayInputStream data = new ByteArrayInputStream("foobarbaz".getBytes());
		Session session = mock(Session.class);
		SessionFactory factory = mock(SessionFactory.class);
		when(factory.getSession()).thenReturn(session);
		when(session.readRaw("foo.txt")).thenReturn(data);
		when(session.finalizeRaw()).thenReturn(true);

		StepExecution stepExecution = new StepExecution("foo", null);
		ExecutionContext stepExecutionContext = new ExecutionContext();
		stepExecutionContext.putString("filePath", "foo.txt");
		stepExecution.setExecutionContext(stepExecutionContext);
		StepContext stepContext = new StepContext(stepExecution);
		ChunkContext chunkContext = new ChunkContext(stepContext);

		RemoteFileTemplate template = new RemoteFileTemplate(factory);
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();

		// clean up from old tests
		FileSystem fs = FileSystem.get(configuration);
		Path p = new Path("/qux/foo.txt");
		fs.delete(p, true);
		assertFalse(fs.exists(p));

		RemoteFileToHadoopTasklet tasklet = new RemoteFileToHadoopTasklet(template,
				configuration, "/qux");

		assertEquals(RepeatStatus.FINISHED, tasklet.execute(null, chunkContext));

		assertTrue(fs.exists(p));

		FSDataInputStream stream = fs.open(p);
		byte[] out = new byte[9];
		stream.readFully(out);
		stream.close();
		assertEquals("foobarbaz", new String(out));

		fs.close();
	}

	@Configuration
	static class EmptyConfig {
	}

}
