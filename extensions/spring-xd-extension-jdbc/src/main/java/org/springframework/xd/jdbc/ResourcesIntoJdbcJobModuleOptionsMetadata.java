/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.jdbc;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import org.springframework.xd.jdbc.ResourcesIntoJdbcJobModuleOptionsMetadata.JobImportToJdbcMixin;
import org.springframework.xd.module.options.mixins.BatchJobCommitIntervalOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldDelimiterOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldNamesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobResourcesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.spi.Mixin;

/**
 * Typical class for metadata about jobs that slurp delimited resources into jdbc. Can be used as is or mixed-in if
 * needed.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Michael Minella
 * @author Thomas Risberg
 */
@Mixin({ JdbcConnectionMixin.class, JdbcConnectionPoolMixin.class, BatchJobRestartableOptionMixin.class,
	BatchJobResourcesOptionMixin.class, BatchJobFieldNamesOptionMixin.class, BatchJobFieldDelimiterOptionMixin.class,
	BatchJobCommitIntervalOptionMixin.class, JobImportToJdbcMixin.class, HadoopConfigurationMixin.class })
public class ResourcesIntoJdbcJobModuleOptionsMetadata {

	public static class JobImportToJdbcMixin extends AbstractImportToJdbcOptionsMetadata {

		/**
		 * Sets default which are appropriate for batch jobs.
		 */
		public JobImportToJdbcMixin() {
			tableName = XD_JOB_NAME;
			initializerScript = "init_batch_import.sql";
		}

	}

}
