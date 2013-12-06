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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * @author David Turanski
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
	public void testCreatingTapWithSameNameAsExistingStream_xd299() {
		CommandResult cr = getShell().executeCommand(
				"stream create --name foo --definition \"tap:stream:foo > counter\"");
		System.out.println(cr);
		assertTrue("Failure. CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure. CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("XD116E:unrecognized stream reference 'foo'"));
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
		stream().create("ticktock-in", "%s > queue:foox", source);
		source.postData("blahblah");
	}

	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		HttpSource source = newHttpSource();
		stream().create("ticktock-in", "%s > queue:foo", source);
		stream().create("ticktock-out", "queue:foo > transform --expression=payload.toUpperCase() | log");
		source.postData("blahblah");
	}

	/**
	 * Test a stream that is simply one processor module connecting two named channels.
	 */
	@Test
	public void testProcessorLinkingChannels() throws Exception {
		FileSink sink = newFileSink().binary(true);
		HttpSource source = newHttpSource(9314);
		stream().create("in1", "%s > queue:foo", source);
		stream().create("proc", "queue:foo > transform --expression=payload.toUpperCase() > queue:bar");
		stream().create("out1", "queue:bar > %s", sink);
		source.postData("blahblah");
		assertThat(sink, eventually(hasContentsThat(equalTo("BLAHBLAH"))));

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
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();

		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text:rys}','.')");
		stream().create("s2", "%s | obfuscate | %s", httpSource, sink);

		httpSource.ensureReady().postData("Dracarys!");
		assertThat(sink, eventually(hasContentsThat(equalTo("Draca.!"))));

	}

	@Test
	public void testParameterizedStreamComposition() throws IOException {
		HttpSource httpSource = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().createDontDeploy("obfuscate", "transform --expression=payload.replace('${text}','.')");
		stream().create("s2", "%s | obfuscate --text=aca | %s", httpSource, sink);
		httpSource.ensureReady().postData("Dracarys!");
		assertThat(sink, eventually(hasContentsThat(equalTo("Dr.rys!"))));

	}

	public void testComposedModules() throws IOException {
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("chain",
				"filter --expression=true | transform --expression=payload.replace('abc','...')");
		stream().create("s2", "%s | chain | %s", httpSource, sink);
		httpSource.postData("abcdefghi!");
		assertThat(sink, eventually(hasContentsThat(equalTo("...defghi!"))));

	}

	@Test
	public void testFilteringSource() throws IOException {
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("myFilteringSource",
				"%s | filter --expression=payload.contains('e')", httpSource);
		stream().create("s2", "myFilteringSource | transform --expression=payload.replace('e','.') | %s", sink);
		httpSource.postData("foobar");
		httpSource.postData("hello");
		httpSource.postData("custardpie");
		httpSource.postData("whisk");
		assertThat(sink, eventually(hasContentsThat(equalTo("h.llocustardpi."))));

	}


	@Test
	public void testParameterizedComposedSource() throws IOException {
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		stream().createDontDeploy("myFilteringSource",
				"%s | filter --expression=payload.contains('${word}')", httpSource);
		stream().create("s2", "myFilteringSource --word=foo | %s", sink);
		httpSource.postData("foobar");
		httpSource.postData("hello");
		httpSource.postData("custardfoo");
		httpSource.postData("whisk");
		assertThat(sink, eventually(hasContentsThat(equalTo("foobarcustardfoo"))));

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
		assertThat(sink, eventually(hasContentsThat(equalTo("foobar\ncustardfoo\n"))));

	}


	@Test
	public void testNestedStreamComposition() throws IOException {
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().createDontDeploy("swap", "transform --expression=payload.replaceAll('${from}','${to}')");
		stream().createDontDeploy("abyz", "swap --from=a --to=z | swap --from=b --to=y");
		stream().create("foo", "%s | abyz | filter --expression=true | %s", source, sink);
		stream().create("mytap", "tap:stream:foo.filter > log"); // will log zzyyccxxyyzz
		source.postData("aabbccxxyyzz");
		assertThat(sink, eventually(hasContentsThat(equalTo("zzyyccxxyyzz"))));

	}

	@Test
	public void testTappingModulesVariations() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		HttpSource httpSource = newHttpSource();
		HttpSource httpSource2 = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);
		FileSink tapsink6 = newFileSink().binary(true);

		stream().create("myhttp", "%s | transform --expression=payload.toUpperCase() | %s", httpSource, sink);
		stream().create("mytap3",
				"tap:stream:myhttp > transform --expression=payload.replaceAll('A','.') | %s",
				tapsink3);
		stream().create("mytap5",
				"tap:stream:myhttp.transform > transform --expression=payload.replaceAll('A','.') | %s",
				tapsink5);
		stream().create("mytap6",
				"tap:stream:myhttp.0 > transform --expression=payload.replaceAll('A','.') | %s",
				tapsink6);
		// use the tap channel as a sink. Not very useful currently but will be once we allow users to create pub/sub
		// channels
		// stream().create("mytap7", "%s > tap:stream:myhttp.transform",
		// httpSource2);

		httpSource.ensureReady().postData("Dracarys!");

		assertThat(sink, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(tapsink3, eventually(hasContentsThat(equalTo("Dracarys!"))));

		assertThat(tapsink5, eventually(hasContentsThat(equalTo("DR.C.RYS!"))));

		assertThat(tapsink6, eventually(hasContentsThat(equalTo("Dracarys!"))));


		// httpSource2.ensureReady().postData("TESTPLAN");
		// our tap got the data and transformed appropriately
		// assertThat(tapsink5, eventually(hasContentsThat(equalTo("DR.C.RYS!TESTPL.N"))));

		// other tap did not get data
		// assertThat(tapsink3, eventually(hasContentsThat(equalTo("Dracarys!"))));

	}

	@Test
	public void testTappingWithLabels() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		HttpSource source = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink1 = newFileSink().binary(true);
		stream().create("myhttp", "%s | flibble: transform --expression=payload.toUpperCase() | %s", source, sink);
		stream().create("mytap4",
				"tap:stream:myhttp.flibble > transform --expression=payload.replaceAll('A','.') | %s",
				tapsink1);
		source.ensureReady().postData("Dracarys!");

		assertThat(sink, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(tapsink1, eventually(hasContentsThat(equalTo("DR.C.RYS!"))));

	}

	@Test
	public void testTappingModulesVariationsWithSinkChannel_XD629() throws IOException {
		HttpSource source = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);

		stream().create("myhttp",
				"%s | transform --expression=payload.toUpperCase() | filter --expression=true > queue:foobar", source);
		stream().create("slurp", "queue:foobar > %s", sink);

		// new style tapping, tap --channel=myhttp.0
		stream().create("mytap3",
				"tap:stream:myhttp > transform --expression=payload.replaceAll('r','.') | %s",
				tapsink3);

		// new style tapping, tap --channel=foobar
		// stream().create("mytap5",
		// "tap:stream:myhttp.filter > transform --expression=payload.replaceAll('A','.') | %s",
		// tapsink5);

		source.ensureReady().postData("Dracarys!");

		assertThat(sink, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		// assertThat(tapsink3, eventually(hasContentsThat(equalTo("D.aca.ys!"))));

		// assertThat(tapsink5, eventually(hasContentsThat(equalTo("DR.C.RYS!"))));

	}

	@Test
	public void testUsingLabels() throws IOException {
		FileSink sink1 = newFileSink().binary(true);
		FileSink sink2 = newFileSink().binary(true);
		FileSink sink3 = newFileSink().binary(true);

		HttpSource source = newHttpSource();

		stream().create("myhttp", "%s | flibble: transform --expression=payload.toUpperCase() | %s", source, sink1);
		stream().create("wiretap2",
				"tap:stream:myhttp.transform > transform --expression=payload.replaceAll('a','.') | %s",
				sink2);
		stream().create("wiretap3",
				"tap:stream:myhttp.flibble > transform --expression=payload.replaceAll('a','.') | %s",
				sink3);

		source.ensureReady().postData("Dracarys!");

		assertThat(sink1, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(sink2, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(sink3, eventually(hasContentsThat(equalTo("DRACARYS!"))));
	}

	@Test
	public void testNamedChannels() throws Exception {
		HttpSource source1 = newHttpSource();
		HttpSource source2 = newHttpSource();
		HttpSource source3 = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().create("stream1", "%s > queue:foo", source1);
		stream().create("stream2", "%s > queue:foo", source2);
		stream().create("stream3", "queue:foo > %s", sink);

		source1.ensureReady().postData("Dracarys!");
		source2.ensureReady().postData("testing");

		assertThat(sink, eventually(hasContentsThat(equalTo("Dracarys!testing"))));


		stream().destroyStream("stream2");

		source1.ensureReady().postData("stillup");
		assertThat(sink, eventually(hasContentsThat(equalTo("Dracarys!testingstillup"))));

		stream().create("stream4", "%s > queue:foo", source3);
		source3.ensureReady().postData("newstream");
		assertThat(sink, eventually(hasContentsThat(equalTo("Dracarys!testingstillupnewstream"))));

		stream().destroyStream("stream4");
		stream().destroyStream("stream1");
		stream().destroyStream("stream3");
	}

	@Test
	public void testJsonPath() throws IOException {
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().create("jsonPathStream",
				"%s | transform --expression='#jsonPath(payload, \"$.foo.bar\")' | %s",
				source, sink);
		source.ensureReady().postData("{\"foo\":{\"bar\":\"123\"}}");
		assertThat(sink, eventually(hasContentsThat(equalTo("123"))));

		stream().destroyStream("jsonPathStream");
	}

	@Test
	public void testDestroyTap() {
		stream().create("main", "file | log");
		stream().create("tap", "tap:stream:main > queue:foo");
		stream().destroyStream("main");
		stream().destroyStream("tap");
	}
}
