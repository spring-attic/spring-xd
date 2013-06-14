/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Test;

/**
 * @author David Turanski
 */
public class StreamTests extends AbstractStreamTests {

	@Test
	public void testTap() throws InterruptedException {
		this.deployStream("test1","time | log");
		this.deployStream("tap","tap @ test1 | file");
	}

	@Test
	public void testStreamList() throws InterruptedException {
		String streamName = "listTest1";
		this.deployStream(streamName, "time | log");
		Set<String> streams = streamDeployer.getDeployedStreams();
		assertThat(streams, hasSize(1));
		assertThat(streams, contains(streamName));
	}

	@Test
	public void testStreamListMultiple() throws InterruptedException {
		String streamName1 = "listTest1";
		this.deployStream(streamName1, "time | log");
		String streamName2 = "listTest2";
		this.deployStream(streamName2, "time | log");
		Set<String> streams = streamDeployer.getDeployedStreams();
		assertThat(streams, hasSize(2));
		assertThat(streams, containsInAnyOrder(streamName1, streamName2));
	}

	@Test
	public void testStreamListEmpty() throws InterruptedException {
		Set<String> streams = streamDeployer.getDeployedStreams();
		assertThat(streams, hasSize(0));
	}
}