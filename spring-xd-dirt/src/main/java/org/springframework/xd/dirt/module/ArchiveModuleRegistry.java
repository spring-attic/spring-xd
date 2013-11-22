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

package org.springframework.xd.dirt.module;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.loader.AsciiBytes;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.ParentLastURLClassLoader;


/**
 * A module registry that supports module archives. Each "archive" (either a subfolder or a jar file) residing in the
 * {@code <root>} directory of this registry is considered as a classpath root. Those archives can themselves embed
 * additional classpath roots in their {@code lib/} subfolder (as seen in Spring Boot applications), which will serve as
 * the module classpath.
 * 
 * <p>
 * Actual module definition are looked up using the {@code classpath:/spring-xd/<type>/<name>.xml} pattern.
 * </p>
 * <p>
 * A single archive can contain several module definitions, all sharing the same third party jars on their classpath.
 * </p>
 * 
 * @author Eric Bottard
 */
public class ArchiveModuleRegistry implements ModuleRegistry {

	/**
	 * Convenience class for finding nested archives (archive entries that can be classpath entries).
	 */
	private static final class ArchiveEntryFilter implements EntryFilter {

		private static final AsciiBytes DOT_JAR = new AsciiBytes(".jar");

		@Override
		public boolean matches(Entry entry) {
			return entry.isDirectory() || entry.getName().endsWith(DOT_JAR);
		}
	}

	/**
	 * Convenience class for finding nested archives that have a prefix in their file path (e.g. "lib/").
	 */
	private static final class PrefixMatchingArchiveFilter implements EntryFilter {

		private final ArchiveEntryFilter filter = new ArchiveEntryFilter();

		private final AsciiBytes prefix;

		private PrefixMatchingArchiveFilter(String prefix) {
			this.prefix = new AsciiBytes(prefix);
		}

		@Override
		public boolean matches(Entry entry) {
			return !entry.getName().equals(this.prefix) && entry.getName().startsWith(this.prefix)
					&& this.filter.matches(entry);
		}
	}

	private static final EntryFilter FILTER = new PrefixMatchingArchiveFilter("lib/");

	private static final Pattern CAPTURING_PATTERN = Pattern.compile("^.*/spring-xd/(.+)/(.+)\\.xml$");

	private static final int TYPE_CAPTURING_GROUP = 1;

	private static final int NAME_CAPTURING_GROUP = 2;

	private File root;

	public ArchiveModuleRegistry(File root) {
		this.root = root;
	}

	public ArchiveModuleRegistry(URL root) throws FileNotFoundException {
		this.root = ResourceUtils.getFile(root);
	}


	private URL[] classpathRootsForArchive(Archive archive) throws IOException, MalformedURLException {
		List<Archive> nestedArchives = archive.getNestedArchives(FILTER);
		URL[] urls = new URL[nestedArchives.size() + 1];
		// Add the archive itself first
		urls[0] = archive.getUrl();
		// Then all embedded archives
		for (int i = 0; i < nestedArchives.size(); i++) {
			urls[i + 1] = nestedArchives.get(i).getUrl();
		}
		return urls;
	}

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		List<ModuleDefinition> result = lookupByPattern(String.format("classpath:/spring-xd/%s/%s.xml", moduleType,
				name));
		Assert.isTrue(result.size() <= 1,
				String.format("Found multiple definitions for %s:%s : %s", moduleType, name, result));
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		return lookupByPattern("classpath:/spring-xd/*/*.xml");
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		return lookupByPattern(String.format("classpath:/spring-xd/%s/*.xml", type.name()));

	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		return lookupByPattern(String.format("classpath:/spring-xd/*/%s.xml", name));
	}

	private List<ModuleDefinition> lookupByPattern(String pattern) {
		try {
			List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();

			for (Archive archive : scanArchives()) {
				URL[] urls = classpathRootsForArchive(archive);

				ClassLoader cl = new ParentLastURLClassLoader(urls, ArchiveModuleRegistry.class.getClassLoader());

				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
				Resource[] resources = resolver.getResources(pattern);
				for (Resource resource : resources) {
					if (!resource.exists()) {
						// PathMatchingResourcePatternResolver.getResources() may return a non-existent
						// resource when given a pattern-less glob
						continue;
					}
					String fullPath = resource.getURI().toString();
					Matcher matcher = CAPTURING_PATTERN.matcher(fullPath);
					Assert.isTrue(matcher.matches(), fullPath + " did not match " + CAPTURING_PATTERN);
					ModuleType type = ModuleType.valueOf(matcher.group(TYPE_CAPTURING_GROUP));
					String actualName = matcher.group(NAME_CAPTURING_GROUP);
					result.add(new ModuleDefinition(actualName, type, resource, urls));
				}
			}
			return result;
		}
		catch (IOException e) {
			throw new RuntimeIOException("An error occured while reading from archives", e);
		}
	}

	private List<Archive> scanArchives() {
		try {
			File[] archivePaths = root.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return !pathname.getName().equals(".") &&
							!pathname.getName().equals("..") &&
							(pathname.isDirectory() || pathname.getName().endsWith(".jar"));
				}
			});
			List<Archive> result = new ArrayList<>(archivePaths.length);
			for (File file : archivePaths) {
				result.add(file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
			}
			return result;
		}
		catch (IOException e) {
			throw new RuntimeIOException("Could not scan directory for archives", e);
		}
	}

}
