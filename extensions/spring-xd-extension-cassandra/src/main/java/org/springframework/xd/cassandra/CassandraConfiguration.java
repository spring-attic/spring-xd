/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.cassandra;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cassandra.config.CompressionType;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.Session;

/**
 * @author Artem Bilan
 */
@Configuration
public class CassandraConfiguration extends AbstractCassandraConfiguration {

	@Value("${contactPoints}")
	private String contactPoints;

	@Value("${port}")
	private int port;

	@Value("${keyspace:}")
	private String keyspace;

	@Value("${username:}")
	private String username;

	@Value("${password:}")
	private String password;

	@Value("${initScript:}")
	private Resource initScript;

	@Value("${entityBasePackages}")
	private String[] entityBasePackages;

	@Value("${compressionType}")
	private CompressionType compressionType;

	@Value("${metricsEnabled}")
	private boolean metricsEnabled;

	@Autowired
	private Session session;

	@PostConstruct
	public void init() throws IOException {
		if (this.initScript != null) {
			String scripts = new Scanner(this.initScript.getInputStream(), "UTF-8").useDelimiter("\\A").next();

			CqlTemplate template = new CqlTemplate(this.session);

			for (String script : StringUtils.delimitedListToStringArray(scripts, ";", "\r\n\f")) {
				if (StringUtils.hasText(script)) { // an empty String after the last ';'
					template.execute(script + ";");
				}
			}
		}
	}

	@Override
	protected String getContactPoints() {
		return this.contactPoints;
	}

	@Override
	protected int getPort() {
		return this.port;
	}

	@Override
	protected String getKeyspaceName() {
		return this.keyspace;
	}

	@Override
	protected AuthProvider getAuthProvider() {
		if (StringUtils.hasText(this.username)) {
			return new PlainTextAuthProvider(this.username, this.password);
		}
		else {
			return null;
		}
	}

	@Override
	public SchemaAction getSchemaAction() {
		return !ObjectUtils.isEmpty(this.entityBasePackages) ? SchemaAction.CREATE : super.getSchemaAction();
	}

	@Override
	public String[] getEntityBasePackages() {
		return this.entityBasePackages;
	}

	@Override
	protected CompressionType getCompressionType() {
		return this.compressionType;
	}

	@Override
	protected boolean getMetricsEnabled() {
		return this.metricsEnabled;
	}

	@Override
	protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
		if (ObjectUtils.isEmpty(this.entityBasePackages) || this.initScript != null) {
			return Collections.singletonList(CreateKeyspaceSpecification.createKeyspace(getKeyspaceName())
					.withSimpleReplication()
					.ifNotExists());
		}
		else {
			return super.getKeyspaceCreations();
		}
	}

}

