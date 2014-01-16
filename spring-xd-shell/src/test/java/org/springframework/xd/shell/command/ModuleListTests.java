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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformValues;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

import com.google.common.base.Function;

/**
 * Tests for ModuleList.
 * 
 * @author Florent Biville
 */
public class ModuleListTests {

	@Test(expected = IllegalArgumentException.class)
	public void failsIfModuleCollectionIsNull() {
		new ModuleList(null);
	}

	@Test
	public void rendersTableByType() {
		Table modules = new ModuleList(newArrayList(
				resource("jms", "source", false),
				resource("aggregator", "processor", false),
				resource("avro", "sink", false),
				resource("myfile", "source", true)
				)).renderByType();

		Map<Integer, String> header = headerNames(modules.getHeaders());
		assertThat(header.size(), equalTo(4));
		assertThat(header, allOf(
				hasEntry(1, "    Source"),
				hasEntry(2, "    Processor"),
				hasEntry(3, "    Sink"),
				hasEntry(4, "    Job")
				));

		List<TableRow> rows = modules.getRows();
		assertThat(rows, hasSize(2));

		Iterator<TableRow> iterator = rows.iterator();
		TableRow firstRow = iterator.next();
		assertThat(firstRow.getValue(1), equalTo("    jms"));
		assertThat(firstRow.getValue(2), equalTo("    aggregator"));
		assertThat(firstRow.getValue(3), equalTo("    avro"));

		TableRow secondRow = iterator.next();
		assertThat(secondRow.getValue(1), equalTo("(c) myfile"));
		assertThat(secondRow.getValue(2), equalTo(""));
		assertThat(secondRow.getValue(3), equalTo(""));
	}


	private ModuleDefinitionResource resource(String name, String type, boolean composed) {
		return new ModuleDefinitionResource(name, type, composed);
	}

	private Map<Integer, String> headerNames(Map<Integer, TableHeader> headers) {
		return transformValues(headers, new Function<TableHeader, String>() {

			@Override
			public String apply(TableHeader header) {
				return header.getName();
			}
		});
	}

}
