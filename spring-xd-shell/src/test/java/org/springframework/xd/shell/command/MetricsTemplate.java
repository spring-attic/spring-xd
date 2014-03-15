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

package org.springframework.xd.shell.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.shell.command.fixtures.FieldValueCounterSink;
import org.springframework.xd.shell.command.fixtures.RichGaugeSink;
import org.springframework.xd.test.fixtures.AggregateCounterSink;
import org.springframework.xd.test.fixtures.CounterSink;
import org.springframework.xd.test.fixtures.Disposable;


/**
 * Access point for creation and remembering of metric sink fixtures.
 * 
 * @author Eric Bottard
 */
public class MetricsTemplate implements Disposable {

	private JLineShellComponent shell;

	private List<Disposable> disposables = new ArrayList<Disposable>();

	private Random random = new Random();

	public MetricsTemplate(JLineShellComponent shell) {
		this.shell = shell;
	}

	public CounterSink newCounterSink(String name) {
		return remember(new CounterSink(shell, name));
	}

	public CounterSink newCounterSink() {
		return remember(new CounterSink(shell, randomName()));
	}

	public AggregateCounterSink newAggregateCounterSink(String name) {
		return remember(new AggregateCounterSink(shell, name));
	}

	public AggregateCounterSink newAggregateCounterSink() {
		return remember(new AggregateCounterSink(shell, randomName()));
	}

	public RichGaugeSink newRichGauge(String name) {
		return remember(new RichGaugeSink(shell, name));
	}

	public RichGaugeSink newRichGauge() {
		return remember(new RichGaugeSink(shell, randomName()));
	}

	public FieldValueCounterSink newFieldValueCounterSink(String name, String fieldName) {
		return remember(new FieldValueCounterSink(shell, name, fieldName));
	}

	public FieldValueCounterSink newFieldValueCounterSink(String fieldName) {
		return remember(new FieldValueCounterSink(shell, randomName(), fieldName));
	}

	private String randomName() {
		return "random" + random.nextInt(100000);
	}

	private <T extends Disposable> T remember(T disposable) {
		disposables.add(disposable);
		return disposable;
	}

	@Override
	public void cleanup() {
		Collections.reverse(disposables);
		for (Disposable disposable : disposables) {
			disposable.cleanup();
		}
	}

}
