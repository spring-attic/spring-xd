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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;

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
		HttpSource source = newHttpSource();
		stream().create("ticktock-in", "%s > :foox", source);
		source.postData("blahblah");
	}

	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		HttpSource source = newHttpSource();
		stream().create("ticktock-in", "%s > :foo", source);
		stream().create("ticktock-out", ":foo > transform --expression=payload.toUpperCase() | log");
		source.postData("blahblah");
	}

	/**
	 * Test a stream that is simply one processor module connecting two named channels.
	 */
	@Test
	public void testProcessorLinkingChannels() throws Exception {
		FileSink sink = newFileSink();
		HttpSource source = newHttpSource(9314);
		stream().create("in1", "%s > :foo", source);
		stream().create("proc", ":foo > transform --expression=payload.toUpperCase() > :bar");
		stream().create("out1", ":bar > %s", sink);
		source.postData("blahblah");
		assertEquals("BLAHBLAH\n", sink.getContents());
	}

	@Test
	public void testDefiningSubstream() {
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
	}

	@Test
	public void testUsingSubstream() {
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("s1", "transform --expression=payload.replace('Andy','zzz')");
		stream().create("s2", "%s | s1 | log", httpSource);
		httpSource.ensureReady().postData("fooAndyfoo");
	}

	@Test
	public void testUsingCompositionWithParameterizationAndDefaultValue() throws IOException {
		FileSink sink = newFileSink();
		HttpSource httpSource = newHttpSource();

		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text:rys}','.')");
		stream().create("s2", "%s | obfuscate | %s", httpSource, sink);

		httpSource.ensureReady().postData("Dracarys!");
		assertEquals("Draca.!\n", sink.getContents());
	}

	@Test
	public void testParameterizedStreamComposition() throws IOException {
		HttpSource httpSource = newHttpSource();
		FileSink sink = newFileSink();
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text}','.')");
		stream().create("s2", "%s | obfuscate --text=aca | %s", httpSource, sink);
		httpSource.ensureReady().postData("Dracarys!");
		assertEquals("Dr.rys!\n", sink.getContents());
	}

	public void testComposedModules() throws IOException {
		FileSink sink = newFileSink();
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("chain",
				"filter --expression=true | transform --expression=payload.replace('abc','...')");
		stream().create("s2", "%s | chain | %s", httpSource, sink);
		httpSource.postData("abcdefghi!");
		// TODO reactivate when get to the bottom of the race condition
		assertEquals("...defghi!\n", sink.getContents());
	}

	@Test
	public void testFilteringSource() throws IOException {
		FileSink sink = newFileSink();
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("myFilteringSource",
				"%s | filter --expression=payload.contains('e')", httpSource);
		stream().create("s2", "myFilteringSource | transform --expression=payload.replace('e','.') | %s", sink);
		httpSource.postData("foobar");
		httpSource.postData("hello");
		httpSource.postData("custardpie");
		httpSource.postData("whisk");
		// TODO reactivate when get to the bottom of the race condition
		assertEquals("h.llo\ncustardpi.\n", sink.getContents());
	}


	@Test
	public void testParameterizedComposedSource() throws IOException {
		FileSink sink = newFileSink();
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("myFilteringSource",
				"%s | filter --expression=payload.contains('${word}')", httpSource);
		stream().create("s2", "myFilteringSource --word=foo | %s", sink);
		httpSource.postData("foobar");
		httpSource.postData("hello");
		httpSource.postData("custardfoo");
		httpSource.postData("whisk");
		assertEquals("foobar\ncustardfoo\n", sink.getContents());
	}


	@Test
	public void testComposedStreamThatIsItselfDeployable() throws IOException {
		FileSink sink = newFileSink();
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("myFilteringHttpSource",
				"%s | filter --expression=payload.contains('${word:foo}') | %s", httpSource, sink);
		stream().create("s2", "myFilteringHttpSource", sink);
		httpSource.postData("foobar");
		httpSource.postData("hello");
		httpSource.postData("custardfoo");
		httpSource.postData("whisk");
		assertEquals("foobar\ncustardfoo\n", sink.getContents());
	}


	@Test
	public void testNestedStreamComposition() throws IOException {
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().createDontDeploy("swap", "transform --expression=payload.replaceAll('${from}','${to}')");
		stream().createDontDeploy("abyz", "swap --from=a --to=z | swap --from=b --to=y");
		stream().create("foo", "%s | abyz | filter --expression=true | %s", source, sink);
		tap().create("mytap", "tap foo.filter | log"); // will log zzyyccxxyyzz
		source.postData("aabbccxxyyzz");
		assertEquals("zzyyccxxyyzz", sink.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingModules() throws IOException {
		FileSink sink = newFileSink().binary(true);
		FileSink tapsink = newFileSink().binary(true);
		HttpSource source = newHttpSource();

		stream().create("myhttp", "%s | transform --expression=payload.toUpperCase() | %s", source, sink);
		tap().create("mytap", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s", tapsink);

		source.ensureReady().postData("Dracarys!");

		assertEquals("DRACARYS!", sink.getContents());
		assertEquals("DR.C.RYS!", tapsink.getContents());
	}

	// We might have a problem using a tap called tap
	// Resolver could skip 'tap' as a 'well known module name'
	@Ignore
	@Test
	public void testTapCalledTap() throws IOException {
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		FileSink tapsink = newFileSink().binary(true);

		stream().create("myhttp", "%s | transform --expression=payload.toUpperCase() | %s", source, sink);

		// Fails with recursion issue?
		tap().create("tap", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s", tapsink);
		source.ensureReady().postData("Dracarys!");

		assertEquals("DRACARYS!", sink.getContents());
		assertEquals("DR.C.RYS!", tapsink.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingModulesVariations() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		HttpSource httpSource = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink1 = newFileSink().binary(true);
		FileSink tapsink2 = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink4 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);

		stream().create("myhttp", "%s | transform --expression=payload.toUpperCase() | %s", httpSource, sink);

		tap().create("mytap1", "tap @myhttp | transform --expression=payload.replaceAll('A','.') | %s", tapsink1);
		tap().create("mytap2", "tap @myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s", tapsink2);
		tap().create("mytap3", "tap myhttp | transform --expression=payload.replaceAll('A','.') | %s", tapsink3);
		tap().create("mytap4", "tap myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s", tapsink4);
		tap().create("mytap5", "tap myhttp.transform | transform --expression=payload.replaceAll('A','.') | %s",
				tapsink5);

		httpSource.ensureReady().postData("Dracarys!");

		// TODO reactivate when get to the bottom of the race condition
		assertEquals("DRACARYS!", sink.getContents());
		assertEquals("Dracarys!", tapsink1.getContents());
		assertEquals("DR.C.RYS!", tapsink2.getContents());
		assertEquals("Dracarys!", tapsink3.getContents());
		assertEquals("DR.C.RYS!", tapsink4.getContents());
		assertEquals("DR.C.RYS!", tapsink5.getContents());
	}

	// See https://jira.springsource.org/browse/XD-592
	@Test
	public void testTappingWithLabels() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		HttpSource source = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink1 = newFileSink().binary(true);

		stream().create("myhttp", "%s | flibble: transform --expression=payload.toUpperCase() | %s", source, sink);
		tap().create("mytap4", "tap myhttp.flibble | transform --expression=payload.replaceAll('A','.') | %s", tapsink1);
		source.ensureReady().postData("Dracarys!");

		assertEquals("DRACARYS!", sink.getContents());
		assertEquals("DR.C.RYS!", tapsink1.getContents());
	}

	@Test
	public void testTappingModulesVariationsWithSinkChannel_XD629() throws IOException {
		HttpSource source = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink1 = newFileSink().binary(true);
		FileSink tapsink2 = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink4 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);

		stream().create("myhttp",
				"%s | transform --expression=payload.toUpperCase() | filter --expression=true > :foobar", source);
		stream().create("slurp", ":foobar > %s", sink);

		// old style tapping, tap --channel=myhttp.0
		tap().create("mytap1",
				"tap @myhttp | transform --expression=payload.replaceAll('D','.') | %s",
				tapsink1);

		// old style tapping, tap --channel=myhttp.1
		tap().create("mytap2",
				"tap @myhttp.1 | transform --expression=payload.replaceAll('A','.') | %s",
				tapsink2);

		// new style tapping, tap --channel=myhttp.0
		tap().create("mytap3",
				"tap myhttp | transform --expression=payload.replaceAll('r','.') | %s",
				tapsink3);

		// new style tapping, tap --channel=myhttp.1
		tap().create("mytap4",
				"tap myhttp.1 | transform --expression=payload.replaceAll('S','.') | %s",
				tapsink4);

		// new style tapping, tap --channel=foobar
		tap().create("mytap5", "tap myhttp.filter | transform --expression=payload.replaceAll('A','.') |  %s",
				tapsink5);

		source.ensureReady().postData("Dracarys!");

		// TODO reactivate these when sink checking reliable! If the test
		// is run standalone these will work.
		assertEquals("DRACARYS!", sink.getContents());
		assertEquals(".racarys!", tapsink1.getContents());
		assertEquals("DR.C.RYS!", tapsink2.getContents());
		assertEquals("D.aca.ys!", tapsink3.getContents());
		assertEquals("DRACARY.!", tapsink4.getContents());
		assertEquals("DR.C.RYS!", tapsink5.getContents());
	}

	// XD M2 does not support '>' with tap
	// See https://jira.springsource.org/browse/XD-592
	@Test
	@Ignore
	public void testUsingLabels() throws IOException {
		FileSink sink1 = newFileSink().binary(true);
		FileSink sink2 = newFileSink().binary(true);
		FileSink sink3 = newFileSink().binary(true);

		HttpSource source = newHttpSource();

		stream().create("myhttp", "%s | flibble: transform --expression=payload.toUpperCase() | log", source);
		tap().create("wiretap1", "tap @myhttp.1 | transform --expression=payload.replaceAll('a','.') | %s", sink1);

		tap().create("wiretap2", "tap myhttp.transform > transform --expression=payload.replaceAll('a','.') | %s",
				sink2);
		tap().create("wiretap3", "tap myhttp.flibble > transform --expression=payload.replaceAll('a','.') | %s", sink3);
		source.ensureReady().postData("Dracarys!");

		// TODO verify both logs output DRACARYS!
		assertEquals("DRACARYS!", sink1.getContents());
		assertEquals("DRACARYS!", sink2.getContents());
		assertEquals("DRACARYS!", sink3.getContents());
	}

}
