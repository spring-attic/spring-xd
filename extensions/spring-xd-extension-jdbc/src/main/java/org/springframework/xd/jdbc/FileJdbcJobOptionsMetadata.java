/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.jdbc;

import org.springframework.xd.module.options.mixins.BatchJobDeleteFilesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobSinglestepPartitionSupportOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;


/**
 * Captures options for the {@code filejdbc} job.
 * 
 * @author Eric Bottard
 */
/*
 * Note: Similar to ResourcesIntoJdbcJobModuleOptionsMetadata but adds deletion support, as the source is
 * file based.
 */
@Mixin({ BatchJobDeleteFilesOptionMixin.class, ResourcesIntoJdbcJobModuleOptionsMetadata.class, BatchJobSinglestepPartitionSupportOptionMixin.class })
public class FileJdbcJobOptionsMetadata {

}
