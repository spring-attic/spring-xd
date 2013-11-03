/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.hadoop.support;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.hadoop.store.support.PollingTaskSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

/**
 * Tests for {@code PollingTaskSupport}.
 * 
 * @author Janne Valkealahti
 * 
 */
public class PollingTaskSupportTests {

	@Test
	public void testPollingTask() throws InterruptedException {
		TaskScheduler taskScheduler = new ConcurrentTaskScheduler();
		TaskExecutor taskExecutor = new SyncTaskExecutor();
		TestPollingTaskSupport testPollingTaskSupport = new TestPollingTaskSupport(taskScheduler, taskExecutor,
				TimeUnit.SECONDS, 2);
		testPollingTaskSupport.init();
		testPollingTaskSupport.start();
		Thread.sleep(3000);
		testPollingTaskSupport.stop();
		assertThat(testPollingTaskSupport.counter, is(2));
		Thread.sleep(3000);
		assertThat(testPollingTaskSupport.counter, is(2));
	}

	private static class TestPollingTaskSupport extends PollingTaskSupport<String> {

		public TestPollingTaskSupport(TaskScheduler taskScheduler, TaskExecutor taskExecutor, TimeUnit unit,
				long duration) {
			super(taskScheduler, taskExecutor, unit, duration);
		}

		int counter = 0;

		@Override
		protected String doPoll() {
			return Integer.toString(counter++);
		}

		@Override
		protected void onPollResult(String result) {
		}

	}

}
