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

package org.springframework.xd.dirt.module;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;

/**
 * An extension of ArchiveModuleRegistry that knows how to save uploaded modules (in compressed form).
 *
 * @author Eric Bottard
 */
public class WriteCapableArchiveModuleRegistry extends ArchiveModuleRegistry implements WriteableModuleRegistry {

	public WriteCapableArchiveModuleRegistry(String root) {
		super(root);
	}

	@Override
	public boolean delete(ModuleDefinition definition) {
		try {
			File jar = targetModuleLocation(definition);
			return jar.exists() && !jar.isDirectory() && jar.delete();
		}
		catch (IOException e) {
			throw new RuntimeIOException("Error trying to delete " + definition, e);
		}
	}

	@Override
	public boolean registerNew(ModuleDefinition definition) {
		if (definition instanceof UploadedModuleDefinition) {
			UploadedModuleDefinition uploadedModuleDefinition = (UploadedModuleDefinition) definition;
			try {
				File jar = targetModuleLocation(definition);
				Assert.isTrue(!jar.exists(), "Could not install " + uploadedModuleDefinition + " at location " + jar + " as that file already exists");
				FileCopyUtils.copy(uploadedModuleDefinition.getBytes(), jar);
				return true;
			}
			catch (IOException e) {
				throw new RuntimeIOException("Error trying to save " + uploadedModuleDefinition, e);
			}
		}
		return false;
	}

	private File targetModuleLocation(ModuleDefinition definition) throws IOException {
		Resource resource = getResources(definition.getType().name(), definition.getName(), ".jar")[0];
		File result = resource.getFile();
		File parent = result.getParentFile();
		Assert.isTrue(parent.exists() && parent.isDirectory() || parent.mkdirs(),
				String.format("Could not ensure that '%s' exists and is a directory", parent.getAbsolutePath()));
		return result.getAbsoluteFile();
	}
}
