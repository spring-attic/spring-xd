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
package org.springframework.xd.greenplum.support;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

public class DefaultLoadService implements LoadService {

	private final static Log log = LogFactory.getLog(DefaultLoadService.class);

    private final JdbcTemplate jdbcTemplate;

    public DefaultLoadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        Assert.notNull(jdbcTemplate, "JdbcTemplate must be set");
    }

    @Override
    public void load(LoadConfiguration loadConfiguration) {
    	load(loadConfiguration, null);
    }

    @Override
    public void load(LoadConfiguration loadConfiguration, RuntimeContext context) {
    	String prefix = UUID.randomUUID().toString().replaceAll("-", "_");

        // setup jdbc operations
        CleanableJdbcOperations operations = new CleanableJdbcOperations(jdbcTemplate);

		String sqlCreateTable = SqlUtils.createExternalReadableTable(loadConfiguration, prefix,
                context != null ? context.getLocations() : null);
        String sqlDropTable = SqlUtils.dropExternalReadableTable(loadConfiguration, prefix);

        operations.add(sqlCreateTable, sqlDropTable);

        // throw if failed during the prepare phase,
        // we can't continue
        operations.prepare();
        if(operations.getCreateException() != null) {
            throw operations.getCreateException();
        }

        String sqlInsert = SqlUtils.load(loadConfiguration, prefix);

        // catching exception from the actual task we need to do
        // to get a change to try the cleanup operations.
        // throwing original exception after that.
        DataAccessException dae = null;
        try {
        	log.debug("Running sql via template [" + sqlInsert + "]");
            jdbcTemplate.execute(sqlInsert);
        } catch (DataAccessException e) {
            dae = e;
        } finally {
            operations.clean();
            if(dae != null) {
                throw dae;
            }
        }
    }

}
