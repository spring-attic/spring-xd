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
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;

/**
 * Test stream commands
 * 
 * @author Mark Pollack
 * @author Kashyap Parikh
 * @author Andy Clement
 */
public class StreamCommandTests extends AbstractShellIntegrationTest {

	private static final Log logger = LogFactory
			.getLog(StreamCommandTests.class);

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");
		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());

		cr = getShell().executeCommand("stream list");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());

		Table t = (Table) cr.getResult();
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
		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());

		Table t = (Table) getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand(
				"stream create --definition \"time | log\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				!cr.isSuccess());
		assertTrue(
				"Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage()
						.contains("There is already a stream named 'ticktock'"));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());
	}

	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a stream that doesn't exist");
		CommandResult cr = getShell().executeCommand(
				"stream destroy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				!cr.isSuccess());
		assertTrue(
				"Failure.  CommandResult = " + cr.toString(),
				cr.getException()
						.getMessage()
						.contains(
								"Can't delete stream 'ticktock' because it does not exist"));
	}

	@Test
	public void testStreamCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 tictok streams with --deploy = false");
		CommandResult cr = getShell()
				.executeCommand(
						"stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());

		Table t = (Table) getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell()
				.executeCommand(
						"stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				!cr.isSuccess());
		assertTrue(
				"Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage()
						.contains("There is already a stream named 'ticktock'"));

		t = (Table) getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());
	}

	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		CommandResult cr = getShell()
				.executeCommand(
						"stream create --definition \"time | log\" --name ticktock --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Created new stream 'ticktock'", cr.getResult());

		Table t = (Table) getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream deploy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());

		t = (Table) getShell().executeCommand("stream list").getResult();
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

		t = (Table) getShell().executeCommand("stream list").getResult();
		assertEquals("ticktock", t.getRows().get(0).getValue(1));
		assertEquals("time | log", t.getRows().get(0).getValue(2));

		cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue(cr.isSuccess());
	}

	// This test hangs the server (produces error: dispatcher has no subscribers for channel 'foox')
	@Ignore
	@Test
	public void testNamedChannelSyntax() {
		logger.info("Create ticktock stream");
		executeStreamCreate("ticktock-in", "http > :foox", true);
		executeStreamCreate("ticktock-out", ":foo > log", true);
		
		executeCommand("post httpsource --data blahblah --target http://localhost:9314");
		executeStreamDestroy("ticktock-in","ticktock-out");
	}
	
	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		executeStreamCreate("ticktock-in", "http > :foo", true);
		executeStreamCreate("ticktock-out",
				":foo > transform --expression=payload.toUpperCase() | log", true);
		executeCommand("post httpsource --data blahblah --target http://localhost:9314");
		executeStreamDestroy("ticktock-in","ticktock-out");
	}
	
	@Test
	public void testDefiningSubstream() {
		executeStreamCreate("s1","transform --expression=payload.replace('Andy','zzz')",false);
		executeStreamDestroy("s1");
	}
	
	@Test
	public void testUsingSubstream() {
		executeStreamCreate("s1","transform --expression=payload.replace('Andy','zzz')",false);
		executeStreamCreate("s2","http | s1 | log",true);
		
		executeCommand("post httpsource --data fooAndyfoo --target http://localhost:9314");
		executeStreamDestroy("s1","s2");
	}
	
	@Test
	public void testUsingSubstreamWithParameterizationAndDefaultValue() {
		executeStreamCreate("obfuscate","transform --expression=payload.replace('${text:rys}','.')",false);
		executeStreamCreate("s2","http | obfuscate | log",true);
		executeCommand("post httpsource --data Dracarys! --target http://localhost:9314");
		// TODO verify the output of the 'log' sink is 'Draca.!'
		executeStreamDestroy("obfuscate","s2");
	}
	
	@Test
	public void testUsingSubstreamWithParameterization() {
		executeStreamCreate("obfuscate","transform --expression=payload.replace('${text}','.')",false);
		executeStreamCreate("s2","http | obfuscate --text=aca | log",true);
		executeCommand("post httpsource --data Dracarys! --target http://localhost:9314");
		// TODO verify the output of the 'log' sink is 'Dr.rys!'
		executeStreamDestroy("obfuscate","s2");
	}

	@Test
	public void testSubSubstreams() {
		executeStreamCreate("swap","transform --expression=payload.replaceAll('${from}','${to}')",false);
		executeStreamCreate("abyz","swap --from=a --to=z | swap --from=b --to=y",false);
		executeStreamCreate("foo","http | abyz | log",true);
		executeCommand("post httpsource --data aabbccxxyyzz --target http://localhost:9314");
		// TODO verify log outputs zzyyccxxbbaa
		executeStreamDestroy("swap","abyz","foo");
	}
	
	@Ignore
	@Test
	public void testUsingLabels() {
		executeStreamCreate("myhttp","http | flibble: transform --expression=payload.toUpperCase() | log",true);
//		executeStreamCreate("wiretap","tap @myhttp.1 | transform --expression=payload.replaceAll('a','.') | log",true);
		// These variants of the above (which does work) don't appear to work although they do refer to the same source channel:
		executeStreamCreate("wiretap","tap myhttp.transform > transform --expression=payload.replaceAll('a','.') | log",true);
		executeStreamCreate("wiretap","tap myhttp.flibble > transform --expression=payload.replaceAll('a','.') | log",true);
		
		executeCommand("post httpsource --data Dracarys! --target http://localhost:9314");
		// TODO verify both logs output DRACARYS!
		executeStreamDestroy("myhttp","wiretap");
	}

	//TODO - the methods below should go into an AbstractStreamCommandTest base class.
	
	@Before
	@After
	public void after() {
		//TODO see if DI can be used instead of lookup
		StreamDefinitionRepository streamDefRepo = getStreamServer().getXmlWebApplicationContext().getBean(StreamDefinitionRepository.class);
		streamDefRepo.deleteAll();
		StreamRepository streamRepo = getStreamServer().getXmlWebApplicationContext().getBean(StreamRepository.class);
		streamRepo.deleteAll();
	}
	
	// ---
	
	/**
	 * Execute 'stream destroy' for the supplied stream names
	 */
	private void executeStreamDestroy(String... streamnames) {
		for (String streamname: streamnames) {
			executeCommand("stream destroy --name "+streamname);
		}
	}

	/**
	 * Execute stream create for the supplied stream name/definition, and verify
	 * the command result.
	 */
	private void executeStreamCreate(
			String streamname,
			String streamdefinition,
			boolean deploy) {
		CommandResult cr = executeCommand("stream create --definition \""+
			streamdefinition+"\" --name "+streamname+
				(deploy?"":" --deploy false"));
		assertEquals("Created new stream '"+streamname+"'",cr.getResult());
	}

}
