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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;

/**
 * Test stream commands
 * @author Mark Pollack
 * @author Kashyap Parikh
 *
 */
public class StreamCommandTests extends AbstractShellIntegrationTest {


	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");	
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());
		
		cr = getShell().executeCommand("stream list");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());

		Table t = (Table)cr.getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));
		
		cr = getShell().executeCommand("stream undeploy --name ticktock");
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream 'ticktock'", cr.getResult());

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());
	}
	
	@Test
	public void testStreamCreateDuplicate() throws InterruptedException {
		logger.info("Create tictok stream");
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());
		
		Table t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		CommandResult cr_dup = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr_dup.toString(), !cr_dup.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr_dup.toString(), cr_dup.getException().getMessage().contains("There is already a stream named 'ticktock'"));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());	
	}
	
	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a stream that doesn't exist");
		CommandResult cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.getException().getMessage().contains("Can't delete stream 'ticktock' because it does not exist"));
	}
	
	@Test
	public void testStreamCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 tictok streams with --deploy = false");
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());

		Table t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.getException().getMessage().contains("There is already a stream named 'ticktock'"));

		t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));
		
		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());	
	}
	
	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());
		
		Table t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream deploy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());
		
		t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));
		
		cr = getShell().executeCommand("stream undeploy --name ticktock");
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream 'ticktock'", cr.getResult());
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream deploy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());
		
		t = (Table)getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());	
	}
	

}