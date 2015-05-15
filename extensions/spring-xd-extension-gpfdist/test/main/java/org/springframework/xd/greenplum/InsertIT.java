/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.greenplum;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.xd.greenplum.support.GreenplumLoad;

public class InsertIT extends AbstractLoadTests {

	@Test
	public void testSimpleInsert() {
		context.register(CommonConfig.class);
		context.refresh();
		JdbcTemplate template = context.getBean(JdbcTemplate.class);
		String drop = "DROP TABLE IF EXISTS AbstractLoadTests;";
		String create = "CREATE TABLE AbstractLoadTests (data text);";
		template.execute(drop);
		template.execute(create);

		List<String> data = new ArrayList<String>();
		for (int i = 0; i<10; i++) {
			data.add("DATA" + i + "\n");
		}

		broadcastData(data);

		GreenplumLoad greenplumLoad = context.getBean(GreenplumLoad.class);
		greenplumLoad.load();

		List<Map<String, Object>> queryForList = template.queryForList("SELECT * from AbstractLoadTests;");
		assertThat(queryForList, notNullValue());
		assertThat(queryForList.size(), is(10));
	}

}
