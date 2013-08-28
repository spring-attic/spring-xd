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

package org.springframework.xd.dirt.launcher;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.xd.dirt.container.XDContainer;

/**
 * A Container Launcher for a Local (single node) Container
 * 
 * @author David Turanski
 * @since 1.0
 */
public class LocalContainerLauncher extends AbstractContainerLauncher {

	private final static AtomicLong idSequence = new AtomicLong();

	@Override
	protected String generateId() {
		return String.valueOf(idSequence.getAndIncrement());
	}

	@Override
	protected String getRuntimeInfo(XDContainer container) {
		final StringBuilder runtimeInfo = new StringBuilder();
		runtimeInfo.append("Using local mode");
		return runtimeInfo.toString();
	}

	@Override
	protected void logErrorInfo(Exception exception) {
		exception.printStackTrace(System.err);
	}

}
