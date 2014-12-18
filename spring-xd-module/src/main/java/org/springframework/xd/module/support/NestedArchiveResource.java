/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.module.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.util.AsciiBytes;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a resource as resolved inside a boot {@link org.springframework.boot.loader.archive.Archive}.
 *
 * @author Eric Bottard
 */
public class NestedArchiveResource implements ContextResource{

	private final Archive rootArchive;

	private final Archive nestedArchive;

	private final String path;

	public NestedArchiveResource(Archive rootArchive, String path) {
		Assert.isTrue(path.startsWith("/"), "path '" + path + "' should start with a '/'");
		this.rootArchive = rootArchive;
		this.path = path;
		this.nestedArchive = maybeLocateNestedArchive();
	}

	private Archive maybeLocateNestedArchive() {
		final AsciiBytes strippedPath = new AsciiBytes(path.substring(1));
		try {
			List<Archive> nestedArchives = rootArchive.getNestedArchives(new Archive.EntryFilter() {
				@Override
				public boolean matches(Archive.Entry entry) {
					return entry.getName().equals(strippedPath);
				}
			});
			Assert.isTrue(nestedArchives.size() <= 1, "More than 1 entry matched " + path + " : " + nestedArchives);
			return nestedArchives.size() == 1 ? nestedArchives.get(0) : null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public String getPathWithinContext() {
		return path;
	}

	@Override
	public boolean exists() {
		return nestedArchive != null;
	}

	@Override
	public boolean isReadable() {
		return exists();
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		assertExists();
		return nestedArchive.getUrl();
	}

	@Override
	public URI getURI() throws IOException {
		assertExists();
		try {
			return nestedArchive.getUrl().toURI();
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public File getFile() throws IOException {
		assertExists();
		return new FileSystemResource(nestedArchive.getUrl().toExternalForm()).getFile();
	}

	@Override
	public long contentLength() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long lastModified() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new NestedArchiveResource(rootArchive, pathToUse);
	}

	@Override
	public String getFilename() {
		int slash = path.lastIndexOf('/');
		return path.substring(slash + 1);
	}

	@Override
	public String getDescription() {
		return toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		assertExists();
		return nestedArchive.getUrl().openStream();
	}

	private void assertExists() throws IOException {
		if (nestedArchive == null) {
			throw new IOException("Nested archive at path " + path + " does not exist");
		}
	}

	@Override
	public String toString() {
		return String.format("%s for archive at %s inside %s", getClass().getSimpleName(), path, rootArchive);
	}
}
