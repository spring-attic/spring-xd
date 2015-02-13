/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.module;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * {@link Resource} based implementation of {@link ModuleRegistry} that supports two kinds of modules:
 * <ul>
 * <li>the "simple" case is a sole xml file, located in a "directory" named after the module type, <i>e.g.</i>
 * {@code source/time.xml}</li>
 * <li>the "enhanced" case is made up of a directory, where the application context file lives in a config sub-directory
 * <i>e.g.</i> {@code source/time/config/time.xml} and extra classpath is loaded from jars in a lib subdirectory
 * <i>e.g.</i> {@code source/time/lib/*.jar}</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Eric Bottard
 */
public class ResourceModuleRegistry implements WriteableModuleRegistry, InitializingBean {

	/**
	 * The extension the module 'File' must have if it's not in exploded dir format.
	 */
	public static final String ARCHIVE_AS_FILE_EXTENSION = ".jar";

	/**
	 * The extension to use for storing an uploaded module hash.
	 */
	public static final String HASH_EXTENSION = ".md5";

	private final static String[] SUFFIXES = new String[] {"", ARCHIVE_AS_FILE_EXTENSION};

	final protected static byte[] hexArray = "0123456789ABCDEF".getBytes();

	/**
	 * Whether to require the presence of a hash file for archives. Helps preventing half-uploaded archives from
	 * being picked up.
	 */
	private boolean requireHashFiles = false;

	/**
	 * Whether to attempt to create the directory structure at startup (disable for read-only implementations).
	 */
	private boolean createDirectoryStructure = false;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private String root;

	public ResourceModuleRegistry(String root) {
		this.root = StringUtils.trimTrailingCharacter(root, '/');
	}

	@Override
	public boolean delete(ModuleDefinition definition) {
		try {
			Resource archive = getResources(definition.getType().name(), definition.getName(), ARCHIVE_AS_FILE_EXTENSION).iterator().next();
			if (archive instanceof WritableResource) {
				WritableResource writableResource = (WritableResource) archive;
				WritableResource hashResource = (WritableResource) hashResource(writableResource);
				// Delete hash first
				ExtendedResource.wrap(hashResource).delete();
				return ExtendedResource.wrap(writableResource).delete();
			}
			else {
				return false;
			}
		}
		catch (IOException e) {
			throw new RuntimeIOException("Exception while trying to delete module " + definition, e);
		}

	}

	@Override
	public boolean registerNew(ModuleDefinition definition) {
		if (definition instanceof UploadedModuleDefinition) {
			UploadedModuleDefinition uploadedModuleDefinition = (UploadedModuleDefinition) definition;
			try {
				Resource archive = getResources(definition.getType().name(), definition.getName(), ARCHIVE_AS_FILE_EXTENSION).iterator().next();
				if (archive instanceof WritableResource) {
					WritableResource writableResource = (WritableResource) archive;
					Assert.isTrue(!writableResource.exists(), "Could not install " + uploadedModuleDefinition + " at location " + writableResource + " as that file already exists");

					MessageDigest md = MessageDigest.getInstance("MD5");
					DigestInputStream dis = new DigestInputStream(uploadedModuleDefinition.getInputStream(), md);
					FileCopyUtils.copy(dis, writableResource.getOutputStream());
					WritableResource hashResource = (WritableResource) hashResource(writableResource);
					// Write hash last
					FileCopyUtils.copy(bytesToHex(md.digest()), hashResource.getOutputStream());

					return true;
				}
				else {
					return false;
				}
			}
			catch (IOException | NoSuchAlgorithmException e) {
				throw new RuntimeException("Error trying to save " + uploadedModuleDefinition, e);
			}
		}
		return false;
	}

	private Resource hashResource(Resource moduleResource) throws IOException {
		return moduleResource.createRelative(moduleResource.getFilename() + HASH_EXTENSION);
	}

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			for (String suffix : SUFFIXES) {
				for (Resource resource : getResources(moduleType.name(), name, suffix)) {
					collect(resource, result);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeIOException(String.format("An error occurred trying to locate module '%s:%s'",
					moduleType, name), e);
		}

		return result.size() == 1 ? result.iterator().next() : null;

	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			for (String suffix : SUFFIXES) {
				for (Resource resource : getResources("*", name, suffix)) {
					collect(resource, result);
				}
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		List<ModuleDefinition> result = new ArrayList<>();
		try {
			for (Resource resource : getResources(type.name(), "*", "")) {
				collect(resource, result);
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		List<ModuleDefinition> result = new ArrayList<>();
		try {
			for (Resource resource : getResources("*", "*", "")) {
				collect(resource, result);
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	protected Iterable<Resource> getResources(String moduleType,
			String moduleName, String suffix) throws IOException {
		String path = String.format("%s/%s/%s%s", this.root, moduleType, moduleName, suffix);
		Resource[] resources = this.resolver.getResources(path);
		List<Resource> filtered = new ArrayList<>();
		for (Resource resource : resources) {
			// Sanitize file paths (and force use of FSR, which is WritableResource)
			if (resource instanceof UrlResource && resource.getURL().getProtocol().equals("file")
					|| resource instanceof FileSystemResource) {
				resource = new FileSystemResource(resource.getFile().getCanonicalFile());
			}
			filtered.add(resource);
		}

		return filtered;
	}

	protected void collect(Resource resource, List<ModuleDefinition> holder) throws IOException {
		// ResourcePatternResolver.getResources() may return resources that don't exist when not using wildcards
		if (!resource.exists()) {
			return;
		}

		String path = resource.getURL().getPath();
		if (path.endsWith(HASH_EXTENSION)) {
			return;
		}
		else if (path.endsWith(ARCHIVE_AS_FILE_EXTENSION) && requireHashFiles && !hashResource(resource).exists()) {
			return;
		}
		// URL paths for directories include an extra slash, strip it for now
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		int lastSlash = path.lastIndexOf('/');
		int nextToLastSlash = path.lastIndexOf('/', lastSlash - 1);

		String name = path.substring(lastSlash + 1);
		if (name.endsWith(ARCHIVE_AS_FILE_EXTENSION)) {
			name = name.substring(0, name.length() - ARCHIVE_AS_FILE_EXTENSION.length());
		}
		String typeAsString = path.substring(nextToLastSlash + 1, lastSlash);
		ModuleType type = null;
		try {
			type = ModuleType.valueOf(typeAsString);
		}
		catch (IllegalArgumentException e) {
			// Not an actual type name, skip
			return;
		}
		String locationToUse = resource.getURL().toString();

		ModuleDefinition found = ModuleDefinitions.simple(name, type, locationToUse);

		if (holder.contains(found)) {
			SimpleModuleDefinition one = (SimpleModuleDefinition) found;
			SimpleModuleDefinition two = (SimpleModuleDefinition) holder.get(holder.indexOf(found));
			throw new IllegalStateException(String.format("Duplicate module definitions for '%s:%s' found at '%s' and '%s'",
					found.getType(), found.getName(), one.getLocation(), two.getLocation()));
		}
		else {
			holder.add(found);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (createDirectoryStructure) {
			// Create intermediary folders
			for (ModuleType type : ModuleType.values()) {
				Resource folder = getResources(type.name(), "", "").iterator().next();
				if (!folder.exists()) {
					ExtendedResource.wrap(folder).mkdirs();
				}
			}
		}

	}

	public void setRequireHashFiles(boolean requireHashFiles) {
		this.requireHashFiles = requireHashFiles;
	}

	public void setCreateDirectoryStructure(boolean createDirectoryStructure) {
		this.createDirectoryStructure = createDirectoryStructure;
	}


	private byte[] bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return hexChars;
	}

}