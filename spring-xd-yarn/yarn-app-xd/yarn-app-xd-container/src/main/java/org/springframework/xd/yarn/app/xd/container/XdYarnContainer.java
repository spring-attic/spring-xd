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

package org.springframework.xd.yarn.app.xd.container;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.springframework.yarn.container.AbstractYarnContainer;

/**
 * Custom yarn container for xd containers.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnContainer extends AbstractYarnContainer {

	// TODO: for now just a placeholder

	private final static Log log = LogFactory.getLog(XdYarnContainer.class);

	@Override
	protected void runInternal() {
		log.info("XXXXX runInternal()");
		try {
			FileSystem fs = FileSystem.get(getConfiguration());
			FileStatus fileStatus = fs.getFileStatus(new Path("/xd"));
			log.info("XXXXX runInternal() " + fileStatus.toString());
		}
		catch (IOException e) {
			log.error("error", e);
		}

	}
	// @Override
	// public boolean isWaitCompleteState() {
	// return true;
	// }

}
