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

import org.junit.After;
import org.springframework.xd.shell.AbstractShellIntegrationTest;

/**
 * Provides an @After JUnit lifecycle method that will destroy the definitions that were
 * created by calling executeXXXCreate methods.
 * 
 * @author Andy Clement
 * @author Mark Pollack
 * 
 */
public abstract class AbstractStreamIntegrationTest extends AbstractShellIntegrationTest {

	private StreamCommandTemplate streamOps;

	private TapCommandTemplate tapOps;

	private CounterCommandTemplate counterOps;

	private AggregateCounterCommandTemplate aggOps;

	private RichGaugeCommandTemplate richGaugeOps;

	public AbstractStreamIntegrationTest() {
		streamOps = new StreamCommandTemplate(getShell());
		tapOps = new TapCommandTemplate(getShell());
		counterOps = new CounterCommandTemplate(getShell());
		aggOps = new AggregateCounterCommandTemplate(getShell());
		richGaugeOps = new RichGaugeCommandTemplate(getShell());
	}

	protected StreamCommandTemplate stream() {
		return streamOps;
	}

	protected TapCommandTemplate tap() {
		return tapOps;
	}

	protected CounterCommandTemplate counter() {
		return counterOps;
	}

	protected AggregateCounterCommandTemplate aggCounter() {
		return aggOps;
	}

	protected RichGaugeCommandTemplate richGauge() {
		return richGaugeOps;
	}

	@After
	public void after() {
		stream().destroyCreatedStreams();
		tap().destroyCreatedTaps();
		counter().deleteDefaultCounter();
		aggCounter().deleteDefaultCounter();
		richGauge().deleteDefaultRichGauge();
	}

}