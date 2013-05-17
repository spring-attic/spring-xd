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

package org.springframework.xd.hadoop.fs;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

/**
 * @author Mark Fisher
 */
public class StubFileSystem extends FileSystem {

	@Override
	public FSDataOutputStream append(Path arg0, int arg1, Progressable arg2) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public FSDataOutputStream create(Path arg0, FsPermission arg1,
			boolean arg2, int arg3, short arg4, long arg5, Progressable arg6)
			throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	@Deprecated
	public boolean delete(Path arg0) throws IOException {
		return false;
	}

	@Override
	public boolean delete(Path arg0, boolean arg1) throws IOException {
		return false;
	}

	@Override
	public FileStatus getFileStatus(Path arg0) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public URI getUri() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public Path getWorkingDirectory() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public FileStatus[] listStatus(Path arg0) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public boolean mkdirs(Path arg0, FsPermission arg1) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public FSDataInputStream open(Path arg0, int arg1) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public boolean rename(Path arg0, Path arg1) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void setWorkingDirectory(Path arg0) {
		throw new UnsupportedOperationException("not implemented");
	}

}
