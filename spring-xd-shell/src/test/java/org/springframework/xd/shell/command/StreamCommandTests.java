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

import java.io.IOException;

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
		stream().create(streamName, "time | log");
		stream().undeploy(streamName);
	}

	@Test
	public void testStreamCreateDuplicate() throws InterruptedException {
		logger.info("Create tictock stream");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		stream().create(streamName, streamDefinition);

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
		stream().createDontDeploy(streamName, streamDefinition);

		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"" + streamDefinition + "\" --name " + streamName + " --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is already a stream named 'ticktock'"));

		stream().verifyExists(streamName, streamDefinition);
	}

	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		stream().createDontDeploy(streamName, streamDefinition);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition);

		stream().undeploy(streamName);
		stream().verifyExists(streamName, streamDefinition);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition);

	}

	/*
	 * TODO for test that post data to be verified, use a file sink and verify contents using guava helper method, shell
	 * pulls in guava now.
	 * 
	 * import com.google.common.base.Charsets; import com.google.common.io.Files;
	 * 
	 * String content = Files.toString(new File("/home/x1/text.log"), Charsets.UTF_8); or List<String> lines =
	 * Files.readLines(new File("/file/path/input.txt"), Charsets.UTF_8); and use hamcrest matcher for collections.
	 * assertThat("List equality", list1, equalTo(list2));
	 */

	@Test
	public void testNamedChannelWithNoConsumerShouldBuffer() {
		logger.info("Create ticktock stream");
		stream().create("ticktock-in", "http --port=9314 > :foox");
		httpPostData("http://localhost:9314", "blahblah");
	}

	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		stream().create("ticktock-in", "http --port=9314 > :foo");
		stream().create("ticktock-out", ":foo > transform --expression=payload.toUpperCase() | log");
		httpPostData("http://localhost:9314", "blahblah");
	}

	@Test
	public void testDefiningSubstream() {
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
	}

	@Test
	public void testUsingSubstream() {
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
		stream().create("s2", "http --port=9314 | s1 | log");
		httpPostData("http://localhost:9314", "fooAndyfoo");
	}

	@Test
	public void testUsingSubstreamWithParameterizationAndDefaultValue() {
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text:rys}','.')");
		stream().create("s2", "http --port=9314 | obfuscate | log");
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify the output of the 'log' sink is 'Draca.!'
	}

	@Test
	public void testUsingSubstreamWithParameterization() {
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text}','.')");
		stream().create("s2", "http --port=9314 | obfuscate --text=aca | log");
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify the output of the 'log' sink is 'Dr.rys!'
	}

	@Test
	public void testSubSubstreams() {
		stream().createDontDeploy("swap", "transform --expression=payload.replaceAll('${from}','${to}')");
		stream().createDontDeploy("abyz", "swap --from=a --to=z | swap --from=b --to=y");
		stream().create("foo", "http --port=9314 | abyz | log");
		httpPostData("http://localhost:9314", "aabbccxxyyzz");
		// TODO verify log outputs zzyyccxxbbaa
	}

	// See https://jira.springsource.org/browse/XD-592
	@Ignore
	@Test
	public void testTappingAndChannels() {
		stream().create("myhttp", "http --port=9314 | transform --expression=payload.toUpperCase() | log");

		// Cureently fails with an infinite recursion loop in parser on the following line
		stream().create("tap", "tap @myhttp.1 | log");
		stream().create("tap_new", "tap myhttp.1 > log");
		executeCommand("http post --data Dracarys! --target http://localhost:9314");
		// TODO verify both logs output DRACARYS!
	}

	@Test
	@Ignore
	// See https://jira.springsource.org/browse/XD-592
	public void testUsingLabels() throws IOException {
		FileSink sink1 = newFileSink();
		FileSink sink2 = newFileSink();
		FileSink sink3 = newFileSink();

		stream().create("myhttp", "http --port=9314 | flibble: transform --expression=payload.toUpperCase() | log");
		tap().create("wiretap1", "tap @myhttp.1 | transform --expression=payload.replaceAll('a','.') | %s", sink1);

		// These variants of the above (which does work) don't appear to work although
		// they do refer to the same source channel:
		tap().create("wiretap2", "tap myhttp.transform > transform --expression=payload.replaceAll('a','.') | %s",
				sink2);
		tap().create("wiretap3", "tap myhttp.flibble > transform --expression=payload.replaceAll('a','.') | %s", sink3);
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify both logs output DRACARYS!
		assertEquals("DRACARYS!\n", sink1.getContents());
		assertEquals("DRACARYS!\n", sink2.getContents());
		assertEquals("DRACARYS!\n", sink3.getContents());

	}

}
