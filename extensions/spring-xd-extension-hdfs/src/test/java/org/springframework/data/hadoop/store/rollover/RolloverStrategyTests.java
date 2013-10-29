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

package org.springframework.data.hadoop.store.rollover;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import org.springframework.data.hadoop.store.TestUtils;

public class RolloverStrategyTests {

	@Test
	public void testSizeRolloverStrategy() throws Exception {
		SizeRolloverStrategy strategy = new SizeRolloverStrategy();
		long defSize = TestUtils.readField("DEFAULT_MAX_SIZE", strategy);
		long size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(defSize));

		// parse failure, expect default
		strategy = new SizeRolloverStrategy("10w");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(defSize));

		strategy = new SizeRolloverStrategy(1000);
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(1000l));
		strategy.setSize(1);
		assertThat(strategy.hasRolled(), is(false));
		strategy.setSize(1000);
		assertThat(strategy.hasRolled(), is(true));

		strategy = new SizeRolloverStrategy("1000B");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(1000l));

		strategy = new SizeRolloverStrategy("1000b");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(1000l));

		strategy = new SizeRolloverStrategy("10K");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10240l));
		strategy = new SizeRolloverStrategy("10k");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10240l));
		strategy = new SizeRolloverStrategy("10KB");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10240l));
		strategy = new SizeRolloverStrategy("10kb");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10240l));

		strategy = new SizeRolloverStrategy("10M");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10485760l));
		strategy = new SizeRolloverStrategy("10m");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10485760l));

		strategy = new SizeRolloverStrategy("10G");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10737418240l));
		strategy = new SizeRolloverStrategy("10g");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10737418240l));

		strategy = new SizeRolloverStrategy("10T");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10995116277760l));
		strategy = new SizeRolloverStrategy("10t");
		size = TestUtils.readField("rolloverSize", strategy);
		assertThat(size, is(10995116277760l));

	}

}
