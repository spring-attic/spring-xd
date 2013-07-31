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
package org.springframework.xd.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * Tests for named channels
 * @author Ilayaperumal Gopinathan
 *
 */
public class NamedChannelTests extends AbstractShellIntegrationTest{

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);
	
	private List<String> streams = new ArrayList<String>();
	
	
	@Test
	public void testCreateNamedChannelAsSink(){
		logger.info("Creating stream with named channel 'foo' as sink");
		String stream1 = "namedchanneltest-ticktock";
		// Chosen port 9193 randomly assuming this port is not being used already.
		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"http --port=9193 | transform --expression=payload.toUpperCase() > :foo\" --name "+stream1);
		// Make sure to add the stream name to streams list if,
		// - want to destroy the stream at @After irrespective of test result
		// - don't have destroy stream in the test method
		streams.add(stream1);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream '"+ stream1 +"'", cr.getResult());
	}
	
	@Test
	public void testCreateNamedChannelAsSource(){
		logger.info("Creating stream with named channel 'foo' as source");
		String stream1 = "namedchanneltest-ticktock";
		String stream2 = "namedchanneltest-ticktock-counter";
		String httpPort = "9193";
		String counterName = "namedchanneltest-counter"+ Math.random();
		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"http --port="+httpPort+" | transform --expression=payload.toUpperCase() > :foo\" " +
				"--name "+stream1);
		streams.add(stream1);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream '"+ stream1 +"'", cr.getResult());
		// Create stream with named channel as source
		cr = getShell().executeCommand(
				"stream create --definition \":foo > counter --name="+counterName+"\"" +
						" --name "+stream2);
		streams.add(stream2);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream '"+ stream2 +"'", cr.getResult());
		// Verify the stream by posting data to the stream and check if the counter is created
		cr = getShell().executeCommand("post httpsource --target \"http://localhost:"+httpPort+"\" --data test");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		Table t = (Table)getShell().executeCommand("counter list").getResult();
		assertTrue("Failure running named channel as source",
				t.getRows().contains(new TableRow().addValue(1, counterName)));
		//TODO: we should delete the counter
		
	}
	
	@After
	public void destroyStreams(){
		for (String stream: streams) {
			CommandResult cr = getShell().executeCommand("stream destroy --name "+stream);
			assertTrue(cr.isSuccess());
		}
	}

}
