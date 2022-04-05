/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.mixins.BatchJobCommitIntervalOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldDelimiterOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldNamesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobResourcesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.mixins.IntoMongoDbOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Options for Hdfs to Mongodb export job module.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Thomas Risberg
 */
@Mixin({ BatchJobRestartableOptionMixin.class, BatchJobResourcesOptionMixin.class, BatchJobFieldNamesOptionMixin.class,
	BatchJobFieldDelimiterOptionMixin.class, BatchJobCommitIntervalOptionMixin.class, IntoMongoDbOptionMixin.Job.class,
	HadoopConfigurationMixin.class })
public class HdfsMongodbJobOptionsMetadata {

	private String idField;

	@ModuleOption("the name of the field to use as the identity in MongoDB")
	public void setIdField(String idField) {
		this.idField = idField;
	}

	public String getIdField() {
		return this.idField;
	}

}
