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
				cr.getException().getMessage().contains("There is no stream definition named 'ticktock'"));
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

		stream().verifyExists(streamName, streamDefinition, false);
	}

	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		String streamName = "ticktock";
		String streamDefinition = "time | log";
		stream().createDontDeploy(streamName, streamDefinition);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition, true);

		stream().undeploy(streamName);
		stream().verifyExists(streamName, streamDefinition, false);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition, true);

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

	@Ignore
	@Test
	public void testDefiningSubstream() {
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
	}

	@Ignore
	@Test
	public void testUsingSubstream() {
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
		stream().create("s2", "http --port=9314 | s1 | log");
		httpPostData("http://localhost:9314", "fooAndyfoo");
	}

	@Ignore
	@Test
	public void testUsingSubstreamWithParameterizationAndDefaultValue() {
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text:rys}','.')");
		stream().create("s2", "http --port=9314 | obfuscate | log");
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO verify the output of the 'log' sink is 'Draca.!'
	}

	@Ignore
	@Test
	public void testUsingSubstreamWithParameterization() throws IOException {
		FileSink sink = newFileSink();
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text}','.')");
		stream().create("s2", "http --port=9314 | obfuscate --text=aca | %s", sink);
		httpPostData("http://localhost:9314", "Dracarys!");
		// TODO reactivate when get to the bottom of the race condition
		// assertEquals("Dr.rys!\n", sink.getContents());
	}

	@Ignore
	@Test
	public void testSubSubstreams() throws IOException {
		FileSink sink = newFileSink();
		stream().createDontDeploy("swap", "transform --expression=payload.replaceAll('${from}','${to}')");
		stream().createDontDeploy("abyz", "swap --from=a --to=z | swap --from=b --to=y");
		stream().create("foo", "http --port=9314 | abyz | %s", sink);
		httpPostData("http://localhost:9314", "aabbccxxyyzz");
		assertEquals("zzyyccxxyyzz\n", sink.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingModules() throws IOException {
		FileSink sink = newFileSink();
		FileSink tapsink = newFileSink();

		stream().create("myhttp", "http --port=9314 | transform --expression=payload.toUpperCase() | %s", sink);
		tap().create("mytap", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s", tapsink);
		executeCommand("http post --data Dracarys! --target http://localhost:9314");

		// TODO reactivate when get to the bottom of the race condition
		// assertEquals("DRACARYS!\n", sink.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink.getContents());
	}

	// We might have a problem using a tap called tap
	// Resolver could skip 'tap' as a 'well known module name'
	@Ignore
	@Test
	public void testTapCalledTap() throws IOException {
		FileSink sink = newFileSink();
		FileSink tapsink = newFileSink();
		stream().create("myhttp", "http --port=9314 | transform --expression=payload.toUpperCase() | %s", sink);

		// Fails with recursion issue?
		tap().create("tap", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s", tapsink);
		executeCommand("http post --data Dracarys! --target http://localhost:9314");

		assertEquals("DRACARYS!\n", sink.getContents());
		assertEquals("DR.C.RYS!\n", tapsink.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingModulesVariations() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		FileSink sink = newFileSink();
		FileSink tapsink1 = newFileSink();
		FileSink tapsink2 = newFileSink();
		FileSink tapsink3 = newFileSink();
		FileSink tapsink4 = newFileSink();
		FileSink tapsink5 = newFileSink();

		stream().create("myhttp", "http --port=9314 | transform --expression=payload.toUpperCase() | %s", sink);

		tap().create("mytap1", "tap @myhttp | transform --expression=payload.replaceAll('A','.') | %s", tapsink1);
		tap().create("mytap2", "tap @myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s", tapsink2);
		tap().create("mytap3", "tap myhttp | transform --expression=payload.replaceAll('A','.') | %s", tapsink3);
		tap().create("mytap4", "tap myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s", tapsink4);
		tap().create("mytap5", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s",
				tapsink5);

		executeCommand("http post --data Dracarys! --target http://localhost:9314");

		// TODO reactivate when get to the bottom of the race condition
		// assertEquals("DRACARYS!\n", sink.getContents());
		// assertEquals("Dracarys!\n", tapsink1.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink2.getContents());
		// assertEquals("Dracarys!\n", tapsink3.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink4.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink5.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingWithLabels() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		FileSink sink = newFileSink();
		FileSink tapsink1 = newFileSink();

		stream().create("myhttp", "http --port=9314 | flibble: transform --expression=payload.toUpperCase() | %s", sink);
		tap().create("mytap4", "tap myhttp.flibble | transform --expression=payload.replaceAll('A','.') | %s", tapsink1);
		executeCommand("http post --data Dracarys! --target http://localhost:9314");

		assertEquals("DRACARYS!\n", sink.getContents());
		assertEquals("DR.C.RYS!\n", tapsink1.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Ignore
	@Test
	public void testTappingModulesVariationsWithSinkChannel() throws IOException {
		FileSink sink = newFileSink();
		FileSink tapsink1 = newFileSink();
		FileSink tapsink2 = newFileSink();
		FileSink tapsink3 = newFileSink();
		FileSink tapsink4 = newFileSink();
		FileSink tapsink5 = newFileSink();

		stream().create("myhttp",
				"http --port=9314 | transform --expression=payload.toUpperCase() | filter --expression=true > :foobar");

		// tap().create("mytap1",
		// "tap @myhttp | transform --expression=payload.replaceAll('A','.') | %s",
		// tapsink1);
		// tap().create("mytap2",
		// "tap @myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s",
		// tapsink2);
		// tap().create("mytap3",
		// "tap myhttp | transform --expression=payload.replaceAll('A','.') | %s",
		// tapsink3);
		// tap().create("mytap4",
		// "tap myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s",
		// tapsink4);
		tap().create("mytap5", "tap myhttp.filter | transform --expression=payload.replaceAll('A','.') | %s", tapsink5);

		executeCommand("http post --data Dracarys! --target http://localhost:9314");

		// assertEquals("DRACARYS!\n", sink.getContents());
		// assertEquals("Dracarys!\n", tapsink1.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink2.getContents());
		// assertEquals("Dracarys!\n", tapsink3.getContents());
		// assertEquals("DR.C.RYS!\n", tapsink4.getContents());
		assertEquals("DR.C.RYS!\n", tapsink5.getContents());
	}

	// XD M2 does not support '>' with tap
	@Ignore
	@Test
	// See https://jira.springsource.org/browse/XD-592
	public void testUsingLabels() throws IOException {
		FileSink sink1 = newFileSink();
		FileSink sink2 = newFileSink();
		FileSink sink3 = newFileSink();

		stream().create("myhttp", "http --port=9314 | flibble: transform --expression=payload.toUpperCase() | log");
		tap().create("wiretap1", "tap @myhttp.1 | transform --expression=payload.replaceAll('a','.') | %s", sink1);

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
