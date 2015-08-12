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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.test.fixtures.FileSink;

/**
 * Test stream commands
 *
 * @author Mark Pollack
 * @author Kashyap Parikh
 * @author Andy Clement
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class AbstractStreamCommandsTests extends AbstractStreamIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractStreamCommandsTests.class);

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		stream().undeploy(streamName);
	}

	@Test
	public void testStreamCreateDuplicate() throws InterruptedException {
		logger.info("Create tictock stream");
		String streamName = generateStreamName();
		String streamDefinition = "time | log";
		stream().create(streamName, streamDefinition);

		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"" + streamDefinition + "\" --name " + streamName);
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is already a stream named '" + streamName + "'"));
	}

	@Test
	public void testCreatingTapWithSameNameAsExistingStream_xd299() {
		String streamName = generateStreamName();
		CommandResult cr = getShell().executeCommand(
				"stream create --name " + streamName + " --definition \"" + getTapName(streamName) + " > counter\"");
		assertTrue("Failure. CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure. CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("unrecognized stream reference '" + streamName + "'"));
	}

	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a stream that doesn't exist");
		String streamName = generateStreamName();
		CommandResult cr = getShell().executeCommand("stream destroy --name " + streamName);
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is no stream definition named '" + streamName + "'"));
	}

	//XD-3346
	@Test
	public void testStreamDestroyAllWithForce() {
		logger.info("Destroy all streams");
		CommandResult cr = getShell().executeCommand("stream all destroy --force");
		assertThat(cr.isSuccess(), is(true));
	}

	@Test
	public void testStreamCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 tictok streams with --deploy = false");
		String streamName = generateStreamName();
		String streamDefinition = "time | log";
		stream().createDontDeploy(streamName, streamDefinition);

		CommandResult cr = getShell().executeCommand(
				"stream create --definition \"" + streamDefinition + "\" --name " + streamName + " --deploy false");
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains("There is already a stream named '" + streamName + "'"));

		stream().verifyExists(streamName, streamDefinition, false);
	}

	@Test
	public void testStreamDeployUndeployFlow() {
		logger.info("Create tictok stream");
		String streamName = generateStreamName();
		String streamDefinition = "time | log";
		stream().createDontDeploy(streamName, streamDefinition);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition, true);

		stream().undeploy(streamName);
		stream().verifyExists(streamName, streamDefinition, false);

		stream().deploy(streamName);
		stream().verifyExists(streamName, streamDefinition, true);
	}

	@Test
	public void testNamedChannelWithNoConsumerShouldBuffer() {
		String streamName = generateStreamName();
		logger.info("Create " + streamName + " stream");
		HttpSource source = newHttpSource();
		stream().create(streamName, "%s > %s", source, generateQueueName());
		source.postData("blahblah");
	}

	@Test
	public void testNamedChannelsLinkingSourceAndSink() {
		HttpSource source = newHttpSource();
		String queue = generateQueueName();
		stream().create(generateStreamName(), "%s > %s", source, queue);
		stream().create(generateStreamName(), "%s > transform --expression=payload.toUpperCase() | log", queue);
		source.postData("blahblah");
	}

	/**
	 * Test a stream that is simply one processor module connecting two named channels.
	 */
	@Test
	public void testProcessorLinkingChannels() throws Exception {
		FileSink sink = newFileSink().binary(true);
		HttpSource source = newHttpSource(9314);

		String streamName0 = generateStreamName();
		String streamName1 = generateStreamName();
		String streamName2 = generateStreamName();

		String queue1 = generateQueueName();
		String queue2 = generateQueueName();

		stream().create(streamName0, "%s > %s", source, queue1);
		stream().create(streamName1, "%s > transform --expression=payload.toUpperCase() > %s", queue1, queue2);
		stream().create(streamName2, "%s > %s", queue2, sink);

		source.postData("blahblah");

		assertThat(String.format("Assert failed for streams %s, %s, %s", streamName0, streamName1, streamName2),
				sink, eventually(hasContentsThat(equalTo("BLAHBLAH"))));
	}

	@Test
	public void testTappingModulesVariations() throws IOException {
		// Note: this test is using a regular sink, not a named channel sink
		HttpSource httpSource = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);
		FileSink tapsink6 = newFileSink().binary(true);

		String streamName = generateStreamName();
		stream().create(streamName, "%s | transform --expression=payload.toUpperCase() | %s", httpSource, sink);
		stream().create(generateStreamName(), "%s > transform --expression=payload.replaceAll('A','.') | %s",
				getTapName(streamName), tapsink3);
		stream().create(generateStreamName(),
				"%s.transform > transform --expression=payload.replaceAll('A','.') | %s", getTapName(streamName),
				tapsink5);
		stream().create(generateStreamName(),
				"%s.0 > transform --expression=payload.replaceAll('A','.') | %s", getTapName(streamName), tapsink6);
		// use the tap channel as a sink. Not very useful currently but will be once we allow users to create pub/sub
		// channels
		// stream().create(getRandomStreamName(), "%s > %s.transform",
		// httpSource2, getTapName(streamName));

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
	public void testTappingModulesVariationsWithSinkChannel_XD629() throws IOException {
		HttpSource source = newHttpSource();

		FileSink sink = newFileSink().binary(true);
		FileSink tapsink3 = newFileSink().binary(true);
		FileSink tapsink5 = newFileSink().binary(true);

		String streamName = generateStreamName();
		String queue = generateQueueName();
		stream().create(streamName,
				"%s | transform --expression=payload.toUpperCase() | filter --expression=true > %s", source, queue);
		stream().create(generateStreamName(), "%s > %s", queue, sink);

		stream().create(generateStreamName(),
				"%s > transform --expression=payload.replaceAll('r','.') | %s", getTapName(streamName), tapsink3);

		stream().create(generateStreamName(),
				"%s.filter > transform --expression=payload.replaceAll('A','.') | %s", getTapName(streamName),
				tapsink5);

		source.ensureReady().postData("Dracarys!");

		assertThat(sink, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(tapsink3, eventually(hasContentsThat(equalTo("D.aca.ys!"))));

		assertThat(tapsink5, eventually(hasContentsThat(equalTo("DR.C.RYS!"))));

	}

	@Test
	public void testUsingLabels() throws IOException {
		FileSink sink1 = newFileSink().binary(true);
		FileSink sink2 = newFileSink().binary(true);

		HttpSource source = newHttpSource();

		String streamName = generateStreamName();
		stream().create(streamName, "%s | flibble: transform --expression=payload.toUpperCase() | %s", source, sink1);
		stream().create(generateStreamName(),
				"%s.flibble > transform --expression=payload.replaceAll('a','.') | %s", getTapName(streamName), sink2);

		source.ensureReady().postData("Dracarys!");

		assertThat(sink1, eventually(hasContentsThat(equalTo("DRACARYS!"))));

		assertThat(sink2, eventually(hasContentsThat(equalTo("DRACARYS!"))));
	}

	@Test
	public void testMaskingOfPasswords() throws IOException {

		String streamName = generateStreamName();
		stream().createDontDeploy(streamName, "http | jdbc --password=mypassword");

		CommandResult cr = getShell().executeCommand("stream list");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertTrue(table.getRows().contains(
				new TableRow().addValue(1, streamName).addValue(2, "http | jdbc --password=**********").addValue(3,
						"undeployed")));
	}

	@Test
	public void testNamedChannels() throws Exception {
		HttpSource source1 = newHttpSource();
		HttpSource source2 = newHttpSource();
		HttpSource source3 = newHttpSource();
		FileSink sink = newFileSink().binary(true);

		String streamName0 = generateStreamName();
		String streamName1 = generateStreamName();
		String streamName2 = generateStreamName();

		String queue = generateQueueName();

		stream().create(streamName0, "%s > %s", source1, queue);
		stream().create(streamName1, "%s > %s", source2, queue);
		stream().create(streamName2, "%s > %s", queue, sink);

		source1.ensureReady().postData("Dracarys!");
		source2.ensureReady().postData("testing");

		assertThat(String.format("Assert failed for streams %s, %s, %s", streamName0, streamName1, streamName2),
				sink, eventually(hasContentsThat(equalTo("Dracarys!testing"))));

		stream().destroyStream(streamName1);

		source1.ensureReady().postData("stillup");
		assertThat(sink, eventually(hasContentsThat(equalTo("Dracarys!testingstillup"))));

		stream().create(generateStreamName(), "%s > %s", source3, queue);
		source3.ensureReady().postData("newstream");
		assertThat(sink, eventually(hasContentsThat(equalTo("Dracarys!testingstillupnewstream"))));
	}

	@Test
	@Ignore("Failing on CI server - Investigate")
	public void testJsonPath() throws IOException {
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --expression='#jsonPath(payload, \"$.foo.bar\")' | %s",
				source, sink);
		source.ensureReady().postData("{\"foo\":{\"bar\":\"123\"}}");
		assertThat(sink, eventually(40, 100, hasContentsThat(equalTo("123"))));
	}

	@Test
	public void testDestroyTap() {
		HttpSource source = newHttpSource();
		String streamName = generateStreamName();
		String tapName = generateStreamName();
		stream().create(streamName, "%s | log", source);
		stream().create(tapName, "%s > %s", getTapName(streamName), generateQueueName());
		stream().destroyStream(streamName);
		stream().destroyStream(tapName);
	}

	@Test
	public void testTapCreateAndReDeploy() {
		HttpSource source = newHttpSource();
		FileSink tapSink1 = newFileSink().binary(true);
		FileSink tapSink2 = newFileSink().binary(true);
		String streamName = generateStreamName();
		String tapName1 = generateStreamName();
		String tapName2 = generateStreamName();
		stream().create(streamName, "%s | null", source);
		stream().create(tapName1, "%s > %s", getTapName(streamName), tapSink1);
		source.ensureReady().postData("123");
		assertThat(tapSink1, eventually(hasContentsThat(equalTo("123"))));
		stream().undeploy(tapName1);
		stream().create(tapName2, "%s > %s", getTapName(streamName), tapSink2);
		source.ensureReady().postData("234");
		assertThat(tapSink2, eventually(hasContentsThat(equalTo("234"))));
	}

	@Test
	public void testUsingPlaceholdersInsideStreamDefinition() {
		String streamName = generateStreamName();
		HttpSource source = newHttpSource();
		FileSink sink = newFileSink().binary(true);

		stream().create(streamName, "%s | transform --expression=\"'${xd.stream.name}'\" | %s", source, sink);

		source.ensureReady().postData("whatever");
		assertThat(sink, eventually(hasContentsThat(equalTo(streamName))));
	}
}
