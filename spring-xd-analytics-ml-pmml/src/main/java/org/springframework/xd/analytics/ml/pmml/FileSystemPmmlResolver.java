/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.ml.pmml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.springframework.util.Assert;
import org.xml.sax.InputSource;

/**
 * A {@link org.springframework.xd.analytics.ml.pmml.PmmlResolver} that can load a {@link org.dmg.pmml.PMML} definition form the Filesystem.
 *
 * @author Thomas Darimont
 */
public class FileSystemPmmlResolver extends AbstractPmmlResolver {

	private final String pmmlModelRootLocation;

	/**
	 * Creates a new {@link org.springframework.xd.analytics.ml.pmml.FileSystemPmmlResolver}.
	 *
	 * @param pmmlModelRootLocation must not be {@literal null}.
	 */
	public FileSystemPmmlResolver(String pmmlModelRootLocation) {

		Assert.notNull(pmmlModelRootLocation);

		this.pmmlModelRootLocation = pmmlModelRootLocation;
	}

	/**
	 * @param name
	 * @param modelId
	 * @return
	 * @throws Exception
	 */
	@Override
	protected InputSource getPmmlText(String name, String modelId) throws Exception {

		File file = new File(pmmlModelRootLocation + "/" + name + ".pmml.xml");

		if (log.isDebugEnabled()) {
			log.debug("Trying to load pmml from file: " + file.getAbsolutePath());
		}

		return new InputSource(new BufferedInputStream(new FileInputStream(file)));
	}
}
