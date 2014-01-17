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

package org.springframework.xd.dirt.plugins.job;

import static org.springframework.xd.module.ModuleType.job;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PojoModuleOptionsMetadata;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * A {@link ModuleOptionsMetadataResolver} that will dynamically add the module options (such as {@code makeUnique})
 * supported by job modules.
 * 
 * @see JobPlugin
 * @author Eric Bottard
 */
public class JobPluginMetadataResolver implements ModuleOptionsMetadataResolver {


	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
		if (moduleDefinition.getType() == job) {
			return new PojoModuleOptionsMetadata(JobOptions.class);
		}
		else {
			return null;
		}
	}

	public static class JobOptions {

		private boolean makeUnique = true;

		private String numberFormat = "";

		private String dateFormat = "";


		public boolean isMakeUnique() {
			return makeUnique;
		}


		public String getNumberFormat() {
			return numberFormat;
		}


		public String getDateFormat() {
			return dateFormat;
		}


		@ModuleOption("whether always allow re-invocation of this job")
		public void setMakeUnique(boolean makeUnique) {
			this.makeUnique = makeUnique;
		}

		@ModuleOption("the number format to use when parsing numeric parameters")
		public void setNumberFormat(String numberFormat) {
			this.numberFormat = numberFormat;
		}

		@ModuleOption("the date format to use when parsing date parameters")
		public void setDateFormat(String dateFormat) {
			this.dateFormat = dateFormat;
		}


	}

}
