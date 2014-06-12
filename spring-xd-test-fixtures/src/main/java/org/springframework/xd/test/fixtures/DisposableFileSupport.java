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

package org.springframework.xd.test.fixtures;

import java.io.File;
import java.io.IOException;

/**
 * Base class for stream elements that deal with file locations.
 *
 * @author Eric Bottard
 */
public abstract class DisposableFileSupport<F extends DisposableFileSupport<F>> extends AbstractModuleFixture<F>
		implements Disposable {

	/**
	 * How long to wait (max) for a file to appear, in ms.
	 */
	static final int DEFAULT_FILE_TIMEOUT = 1000;

	protected File file;

	protected DisposableFileSupport() {
		this(null);
	}

	protected DisposableFileSupport(File where) {
		try {
			file = File.createTempFile(getClass().getSimpleName(), ".txt", where);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void cleanup() {
		file.delete();
	}

	/**
	 * Utility method to create an empty directory. Caller is responsible for its explicit deletion.
	 */
	public static File makeTempDir() {
		try {
			File dir = File.createTempFile("xd-test", "");
			dir.delete();
			dir.mkdirs();
			return dir;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Wait for a file to appear.
	 *
	 * @param file the file to look for
	 * @param timeout how long to wait until giving up, in ms
	 * @return true if the file is detected
	 */
	public static boolean waitFor(File file, int timeout) {
		long stopAt = System.currentTimeMillis() + timeout;

		while (!file.exists() && System.currentTimeMillis() < stopAt) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		return file.exists();
	}

}
