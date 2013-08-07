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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;

import static org.junit.Assert.*;

/**
 * Test stream commands
 * 
 * @author Mark Pollack
 * @author Kashyap Parikh
 * @author Andy Clement
 */
public class StreamCommandTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");
		String streamName = "ticktock";
		executeStreamCreate(streamName, "time | log");
		executeStreamUndeploy(streamName);
	}

	@Test
	public void testStreamCreateDuplicate() throws InterruptedException {
		logger.info("Create tictock stream");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		executeStreamCreate(streamName, streamDefinition);

		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"" + streamDefinition + "\" --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is already a stream named 'ticktock'"));
	}

	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a stream that doesn't exist");
		CommandResult cr = getShell().executeCommand("stream destroy --name ticktock");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("Can't delete stream 'ticktock' because it does not exist"));
	}

	@Test
	public void testStreamCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 tictok streams with --deploy = false");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		executeStreamCreate(streamName, streamDefinition, false);

		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"" + streamDefinition + "\" --name " + streamName + " --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is already a stream named 'ticktock'"));

		verifyStreamExists(streamName, streamDefinition);
	}

	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		executeStreamCreate(streamName, streamDefinition, false);

		executeStreamDeploy(streamName);
		verifyStreamExists(streamName, streamDefinition);

		executeStreamUndeploy(streamName);
		verifyStreamExists(streamName, streamDefinition);

		executeStreamDeploy(streamName);
		verifyStreamExists(streamName, streamDefinition);

	}

	/*
	 * TODO for test that post data to be verified, use a file sink and verify contents
	 * using guava helper method, shell pulls in guava now.
	 * 
	 * import com.google.common.base.Charsets; import com.google.common.io.Files;
	 * 
	 * String content = Files.toString(new File("/home/x1/text.log"), Charsets.UTF_8); or
	 * List<String> lines = Files.readLines(new File("/file/path/input.txt"),
	 * Charsets.UTF_8); and use hamcrest matcher for collections.
	 * assertThat("List equality", list1, equalTo(list2));
	 */

	// This test hangs the server (produces error: dispatcher has no subscribers for
	// channel 'foox')
	@Ignore
	@Test
	public void testNamedChannelSyntax() {
		logger.info("Create ticktock stream");
		executeStreamCreate("ticktock-in", "http --port=9314 > :foox", true);
		executeStreamCreate("ticktock-out", ":foo > log", true);
		httpPostData("http://localhost:9314", "blahblah");
	}

	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		executeStreamCreate("ticktock-in", "http --port=9314 > :foo", true);
		executeStreamCreate("ticktock-out", ":foo > transform --expression=payload.toUpperCase() | log", true);
		httpPostData("http://localhost:9314", "blahblah");
	}

	@Test
	public void testDefiningSubstream() {
		executeStreamCreate("s1", "transform --expression=payload.replace('Andy','zzz')", false);
	}

	@Test
	public void testUsingSubstream() {
		executeStreamCreate("s1", "transform --expression=payload.replace('Andy','zzz')", false);
		executeStreamCreate("s2", "http --port=9314 | s1 | log", true);
		httpPostData("http://localhost:9314", "fooAndyfoo");
	}

	@Test
	public void testUsingSubstreamWithParameterizationAndDefaultValue() {
		executeStreamCreate("obfuscate", "transform --expression=payload.replace('${text:rys}','.')", false);
		executeStreamCreate("s2", "http --port=9314 | obfuscate | log", true);
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify the output of the 'log' sink is 'Draca.!'
	}

	@Test
	public void testUsingSubstreamWithParameterization() {
		executeStreamCreate("obfuscate", "transform --expression=payload.replace('${text}','.')", false);
		executeStreamCreate("s2", "http --port=9314 | obfuscate --text=aca | log", true);
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify the output of the 'log' sink is 'Dr.rys!'
	}

	@Test
	public void testSubSubstreams() {
		executeStreamCreate("swap", "transform --expression=payload.replaceAll('${from}','${to}')", false);
		executeStreamCreate("abyz", "swap --from=a --to=z | swap --from=b --to=y", false);
		executeStreamCreate("foo", "http --port=9314 | abyz | log", true);
		httpPostData("http://localhost:9314", "aabbccxxyyzz");
		// TODO verify log outputs zzyyccxxbbaa
	}

	@Ignore
	@Test
	public void testUsingLabels() {
		executeStreamCreate("myhttp", "http --port=9314 | flibble: transform --expression=payload.toUpperCase() | log",
				true);
		// executeStreamCreate("wiretap","tap @myhttp.1 | transform --expression=payload.replaceAll('a','.') | log",true);
		// These variants of the above (which does work) don't appear to work although
		// they do refer to the same source channel:
		executeStreamCreate("wiretap",
				"tap myhttp.transform > transform --expression=payload.replaceAll('a','.') | log", true);
		executeStreamCreate("wiretap", "tap myhttp.flibble > transform --expression=payload.replaceAll('a','.') | log",
				true);
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify both logs output DRACARYS!
	}

}
