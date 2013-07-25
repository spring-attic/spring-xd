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
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

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
		
		//TableRow row =  new TableRow().addValue(0, "ticktock").addValue(1, "time | log");
		//Table table = new Table().addHeader(0, new TableHeader("Stream Name")).addHeader(1, new TableHeader("Stream Definition"));
		//table.getRows().add(row);
		//final String expectedTableAsString = UiUtils.renderTextTable(table);
		
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)cr.getResult()));

		cr = getShell().executeCommand("stream undeploy --name ticktock");
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream 'ticktock'", cr.getResult());
		//Let two ticks pass...
		Thread.sleep(2000);
		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());
	}
	
	@Test
	public void testStreamCreateDuplicate() throws InterruptedException {
		logger.info("Create tictok stream");
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));

		CommandResult cr_dup = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr_dup.toString(), !cr_dup.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr_dup.toString(), cr_dup.getException().getMessage().contains("There is already a stream named 'ticktock'"));

		//Let two ticks pass...
		Thread.sleep(2000);
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
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));

		cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.getException().getMessage().contains("There is already a stream named 'ticktock'"));
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));
		
		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());	
	}
	
	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		CommandResult cr = getShell().executeCommand("stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));

		cr = getShell().executeCommand("stream deploy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));
		
		cr = getShell().executeCommand("stream undeploy --name ticktock");
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream 'ticktock'", cr.getResult());
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));

		cr = getShell().executeCommand("stream deploy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());
		assertEquals(getTicktockTable(), UiUtils.renderTextTable((Table)getShell().executeCommand("stream list").getResult()));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());	
	}
	

}