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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Spring ResourceLoader that can locate nested boot archives inside a root archive.
 *
 * @author Eric Bottard
 */
public class ArchiveResourceLoader extends DefaultResourceLoader implements ResourcePatternResolver {

	private final Archive archive;

	private final PathMatcher pathMatcher = new AntPathMatcher();


	public ArchiveResourceLoader(Archive archive) {
		this.archive = archive;
	}

	@Override
	public Resource[] getResources(final String locationPattern) throws IOException {
		final List<String> paths = new ArrayList<String>();
		archive.getNestedArchives(new Archive.EntryFilter() {
			@Override
			public boolean matches(Archive.Entry entry) {
				String path = "/" + entry.getName().toString();
				boolean match = pathMatcher.match(locationPattern, path);
				if (match) {
					paths.add(path);
				}
				return match;
			}
		});
		Resource[] result = new Resource[paths.size()];
		int i = 0;
		for (String path : paths) {
			result[i++] = new NestedArchiveResource(archive, path);
		}
		return result;
	}

	@Override
	protected Resource getResourceByPath(final String path) {
		if (!path.startsWith("/")) {
			return super.getResourceByPath(path);
		} else {
			return new NestedArchiveResource(archive, path);
		}

	}



}
