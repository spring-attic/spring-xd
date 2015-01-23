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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.hadoop.configuration.ConfigurationFactoryBean;
import org.springframework.data.hadoop.fs.HdfsResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Writable extension of {@link ResourceModuleRegistry}.
 *
 * <p>Will generate MD5 hash files for written modules.</p>
 *
 * @author Eric Bottard
 */
public class WritableResourceModuleRegistry extends ResourceModuleRegistry implements WritableModuleRegistry, InitializingBean {

	final protected static byte[] HEX_DIGITS = "0123456789ABCDEF".getBytes();

	/**
	 * Whether to attempt to create the directory structure at startup (disable for read-only implementations).
	 */
	private boolean createDirectoryStructure = true;


	public WritableResourceModuleRegistry(String root) {
		super(root);
		setRequireHashFiles(true);
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
		if (!(definition instanceof UploadedModuleDefinition)) {
			return false;
		}
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
	@Override
	public void afterPropertiesSet() throws Exception {
		if (root.startsWith("hdfs:")) {
			ConfigurationFactoryBean configurationFactoryBean = new ConfigurationFactoryBean();
			configurationFactoryBean.setRegisterUrlHandler(true);
			configurationFactoryBean.setFileSystemUri(root);
			configurationFactoryBean.afterPropertiesSet();

			this.resolver = new HdfsResourceLoader(configurationFactoryBean.getObject());
		}

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

	public void setCreateDirectoryStructure(boolean createDirectoryStructure) {
		this.createDirectoryStructure = createDirectoryStructure;
	}

	private byte[] bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_DIGITS[v >>> 4];
			hexChars[j * 2 + 1] = HEX_DIGITS[v & 0x0F];
		}
		return hexChars;
	}
}