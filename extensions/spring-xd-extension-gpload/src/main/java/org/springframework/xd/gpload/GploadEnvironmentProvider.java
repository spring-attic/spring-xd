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

package org.springframework.xd.gpload;

import org.springframework.batch.step.tasklet.x.EnvironmentProvider;

import java.io.File;
import java.util.Map;

/**
 * Class that sets environment variables for gpload
 */
public class GploadEnvironmentProvider implements EnvironmentProvider {

	String gploadHome;

	boolean usePasswordFile;

	String passwordInfo;

	public GploadEnvironmentProvider(String gploadHome, boolean usePasswordFile, String passwordInfo) {
		this.gploadHome = gploadHome;
		this.usePasswordFile = usePasswordFile;
		this.passwordInfo = passwordInfo;
	}

	@Override
	public void setEnvironment(Map<String, String> env) {
		File unixPath = new File(gploadHome + File.separator + "greenplum_loaders_path.sh");
		if (unixPath.exists()) {
			env.put("GPHOME_LOADERS", gploadHome);
		}
		else {
			unixPath = new File(gploadHome + File.separator + "greenplum_path.sh");
			if (unixPath.exists()) {
				env.put("GPHOME", gploadHome);
			}
			else {
				throw new IllegalArgumentException("The provided --gploadHome '" + gploadHome + "' does not point to a valid installation");
			}
		}
		unixPath = null;
		if (usePasswordFile) {
			env.put("PGPASSFILE", passwordInfo);
		}
		else {
			env.put("PGPASSWORD", passwordInfo);
		}
	}
}
