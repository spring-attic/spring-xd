/*
 * Copyright 2013-2015 the original author or authors.
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

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.Disposable;
import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.fixtures.FileSource;
import org.springframework.xd.test.fixtures.FtpSink;
import org.springframework.xd.test.fixtures.FtpSource;
import org.springframework.xd.test.fixtures.JdbcSink;
import org.springframework.xd.test.fixtures.JdbcSource;
import org.springframework.xd.test.fixtures.MailSink;
import org.springframework.xd.test.fixtures.NonPollingImapSource;
import org.springframework.xd.test.fixtures.PollingMailSource;
import org.springframework.xd.test.fixtures.TailSource;
import org.springframework.xd.test.fixtures.TcpSink;
import org.springframework.xd.test.fixtures.TcpSource;


/**
 * Provides an @After JUnit lifecycle method that will destroy the definitions that were created by the test.
 *
 * @author Andy Clement
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Franck Marchand
 */
public abstract class AbstractStreamIntegrationTest extends AbstractShellIntegrationTest {

	private StreamCommandsTemplate streamOps;

	private List<Disposable> disposables = new ArrayList<Disposable>();

	private MetricsTemplate metrics;

	private ModuleTemplate moduleTemplate;

	public AbstractStreamIntegrationTest() {
		streamOps = new StreamCommandsTemplate(getShell(), integrationTestSupport);
		metrics = new MetricsTemplate(getShell());
		moduleTemplate = new ModuleTemplate(getShell());
		disposables.add(metrics);
		disposables.add(moduleTemplate);
	}

	protected MetricsTemplate metrics() {
		return metrics;
	}

	protected StreamCommandsTemplate stream() {
		return streamOps;
	}

	protected ModuleTemplate module() {
		return moduleTemplate;
	}

	@After
	public void after() {
		stream().destroyCreatedStreams();
		// Clean up composed modules AFTER streams
		cleanUpDisposables();

	}

	private void cleanUpDisposables() {
		Collections.reverse(disposables);
		for (Disposable disposable : disposables) {
			disposable.cleanup();
		}
	}

	protected TcpSource newTcpSource() {
		return new TcpSource();
	}

	protected TcpSink newTcpSink() {
		TcpSink tcpSink = new TcpSink();
		disposables.add(tcpSink);
		return tcpSink;
	}

	protected JdbcSink newJdbcSink() {
		JdbcSink jdbcSink = new JdbcSink(createDataSource());
		disposables.add(jdbcSink);
		return jdbcSink;
	}

	protected JdbcSource newJdbcSource() {
		JdbcSource jdbcSource = new JdbcSource(createDataSource());
		disposables.add(jdbcSource);
		return jdbcSource;
	}

	protected FtpSource newFtpSource() {
		FtpSource ftpSource = new FtpSource();
		disposables.add(ftpSource);
		return ftpSource;
	}

	protected FtpSink newFtpSink() {
		FtpSink ftpSink = new FtpSink();
		disposables.add(ftpSink);
		return ftpSink;
	}

	private DataSource createDataSource() {

		String url = "jdbc:hsqldb:mem:%s";
		String dbname = "foo";
		String driver = "org.hsqldb.jdbc.JDBCDriver";

		String jdbcUrl = String.format(url, dbname);
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

		try {
			@SuppressWarnings("unchecked")
			Class<? extends Driver> classz = (Class<? extends Driver>) Class.forName(driver);
			dataSource.setDriverClass(classz);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("failed to load class: " + driver, e);
		}

		dataSource.setUrl(jdbcUrl);
		return dataSource;
	}

	protected FileSink newFileSink() {
		FileSink fileSink = new FileSink();
		disposables.add(fileSink);
		return fileSink;
	}

	protected FileSource newFileSource() {
		FileSource fileSource = new FileSource();
		disposables.add(fileSource);
		return fileSource;
	}

	protected TailSource newTailSource() {
		TailSource tailSource = new TailSource();
		disposables.add(tailSource);
		return tailSource;
	}

	protected PollingMailSource newPollingMailSource() {
		PollingMailSource pollingMailSource = new PollingMailSource();
		disposables.add(pollingMailSource);
		return pollingMailSource;
	}

	protected NonPollingImapSource newNonPollingMailSource() {
		NonPollingImapSource nonPollingImapSource = new NonPollingImapSource();
		disposables.add(nonPollingImapSource);
		return nonPollingImapSource;
	}

	protected MailSink newMailSink() {
		MailSink mailSink = new MailSink();
		disposables.add(mailSink);
		return mailSink;
	}

	protected HttpSource newHttpSource() {
		return new HttpSource(getShell());
	}

	protected HttpSource newHttpSource(int port) {
		return new HttpSource(getShell(), port);
	}

}
