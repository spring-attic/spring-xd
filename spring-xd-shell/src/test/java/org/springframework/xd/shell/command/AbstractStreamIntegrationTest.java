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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.shell.AbstractShellIntegrationTest;

/**
 * Provides an @After JUnit lifecycle method that will destroy the definitions that were created by calling
 * executeXXXCreate methods.
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

	private Set<FileSink> fileSinks = new HashSet<AbstractStreamIntegrationTest.FileSink>();

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
		tap().destroyCreatedTaps();
		stream().destroyCreatedStreams();
		counter().deleteDefaultCounter();
		aggCounter().deleteDefaultCounter();
		richGauge().deleteDefaultRichGauge();
		cleanFileSinks();
	}

	private void cleanFileSinks() {
		for (FileSink fileSink : fileSinks) {
			fileSink.cleanup();
		}
	}

	protected FileSink newFileSink() {
		FileSink fileSink = new FileSink();
		fileSinks.add(fileSink);
		return fileSink;
	}

	protected static class FileSink {

		private File file;

		/**
		 * Constructs a new File Sink with a generated temp file.
		 */
		public FileSink() {
			try {
				file = File.createTempFile("xd-test", "txt");
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * Returns a representation of the sink suitable for inclusion in a stream definition, <i>e.g.</i> @code{file
		 * --dir=xxxx --name=yyyy}
		 */
		@Override
		public String toString() {
			return String.format("file --dir=%s --name=%s", file.getParent(), file.getName());
		}

		public void cleanup() {
			file.delete();
		}

		public String getContents() throws IOException {
			FileReader fileReader = new FileReader(file);
			return FileCopyUtils.copyToString(fileReader);
		}
	}

}