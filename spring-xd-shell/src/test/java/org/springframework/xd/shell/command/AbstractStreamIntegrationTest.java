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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.springframework.util.FileCopyUtils;
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

	private FieldValueCounterCommandTemplate fvcOps;

	private RichGaugeCommandTemplate richGaugeOps;

	private Set<FileSink> fileSinks = new HashSet<AbstractStreamIntegrationTest.FileSink>();

	private Set<TailSource> tailSources = new HashSet<AbstractStreamIntegrationTest.TailSource>();

	public AbstractStreamIntegrationTest() {
		streamOps = new StreamCommandTemplate(getShell());
		tapOps = new TapCommandTemplate(getShell());
		counterOps = new CounterCommandTemplate(getShell());
		aggOps = new AggregateCounterCommandTemplate(getShell());
		fvcOps = new FieldValueCounterCommandTemplate(getShell());
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

	protected FieldValueCounterCommandTemplate fvc() {
		return fvcOps;
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
		fvc().deleteDefaultFVCounter();
		richGauge().deleteDefaultRichGauge();
		cleanFileSinks();
		cleanTailSources();
	}

	private void cleanFileSinks() {
		for (FileSink fileSink : fileSinks) {
			fileSink.cleanup();
		}
	}

	private void cleanTailSources() {
		for (TailSource tailSource : tailSources) {
			tailSource.cleanup();
		}
	}

	protected FileSink newFileSink() {
		FileSink fileSink = new FileSink();
		fileSinks.add(fileSink);
		return fileSink;
	}

	protected TailSource newTailSource() {
		TailSource tailSource = new TailSource();
		tailSources.add(tailSource);
		return tailSource;
	}

	/**
	 * Support class to capture output of a sink in a File.
	 * 
	 * @author Eric Bottard
	 */
	protected static class FileSink extends DisposableFileSupport {

		public String getContents() throws IOException {
			FileReader fileReader = new FileReader(file);
			return FileCopyUtils.copyToString(fileReader);
		}

		@Override
		protected String toDSL() {
			return String.format("file --dir=%s --name=%s", file.getParent(), file.getName());
		}

	}

	/**
	 * Support class to inject data as a sink into a stream.
	 * 
	 * @author Ilayaperumal Gopinathan
	 */
	protected static class TailSource extends DisposableFileSupport {

		@Override
		public String toDSL() {
			return String.format("tail --fromEnd=%s --name=%s", "false", file.getAbsolutePath());
		}

		public void appendToFile(String contents) throws IOException {
			FileWriter fileWritter = new FileWriter(file, true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			bufferWritter.write(contents);
			bufferWritter.close();
		}
	}

	protected abstract static class DisposableFileSupport {

		protected File file;

		protected DisposableFileSupport() {
			this(null);
		}

		protected DisposableFileSupport(File where) {
			try {
				file = File.createTempFile(getClass().getSimpleName(), ".txt", where);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public final String toString() {
			return toDSL();
		}

		/**
		 * Returns a representation of the source/sink suitable for inclusion in a stream
		 * definition, <i>e.g.</i> {@code file --dir=xxxx --name=yyyy}
		 */
		protected abstract String toDSL();

		public void cleanup() {
			file.delete();
		}

	}

}