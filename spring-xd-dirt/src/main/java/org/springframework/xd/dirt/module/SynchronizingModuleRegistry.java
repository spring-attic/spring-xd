/*
 *
 *  * Copyright 2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * A ModuleRegistry that is configured with two delegates: a remote (source) and a local (target) registry. This registry will return
 * results that exist in the remote registry but will synchronize them with the local registry. Results returned are always
 * from the <em>local</em> registry (unless composed). Mutative operations go through to the remote repository though.
 *
 * <p>This is useful as reading
 * Boot uber-jars requires local {@code java.io.File} access, but modules may reside in a remote registry (<i>e.g.</i>
 * backed by HDFS). For such a case, simply use this registry as the main registry, configuring it with the {@code hdfs://} registry as
 * its source and the {@code file://} one as its target.</p>
 *
 * @since 1.2
 * @author Eric Bottard
 */
public class SynchronizingModuleRegistry implements WritableModuleRegistry {

	private final WritableModuleRegistry remoteRegistry;

	private final WritableModuleRegistry localRegistry;

	private final StalenessCheck staleness = new MD5StalenessCheck();

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public SynchronizingModuleRegistry(WritableModuleRegistry remoteRegistry, WritableModuleRegistry localRegistry) {
		Assert.notNull(remoteRegistry, "remoteRegistry cannot be null");
		Assert.notNull(localRegistry, "localRegistry cannot be null");
		this.remoteRegistry = remoteRegistry;
		this.localRegistry = localRegistry;
	}


	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		ModuleDefinition remoteDefinition = remoteRegistry.findDefinition(name, moduleType);
		if (remoteDefinition == null || remoteDefinition.isComposed()) {
			return remoteDefinition;
		}

		copyIfStale((SimpleModuleDefinition) remoteDefinition);
		return currentLocalDefinition(remoteDefinition);
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		List<ModuleDefinition> remoteDefinitions = remoteRegistry.findDefinitions(name);
		return refresh(remoteDefinitions);
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		List<ModuleDefinition> remoteDefinitions = remoteRegistry.findDefinitions(type);
		return refresh(remoteDefinitions);
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		List<ModuleDefinition> remoteDefinitions = remoteRegistry.findDefinitions();
		return refresh(remoteDefinitions);
	}

	/**
	 * Return a refreshed list of definitions from the target registry.
	 */
	private List<ModuleDefinition> refresh(List<ModuleDefinition> remoteDefinitions) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>(remoteDefinitions.size());
		for (ModuleDefinition remoteDefinition : remoteDefinitions) {
			if (remoteDefinition.isComposed()) {
				result.add(remoteDefinition);
			}
			else {
				copyIfStale((SimpleModuleDefinition) remoteDefinition);
				result.add(currentLocalDefinition(remoteDefinition));
			}
		}

		return result;
	}


	/**
	 * Checks whether the target version of the definition is stale, and if it is, triggers an update (as a deletion then
	 * new registration).
	 */
	private synchronized void copyIfStale(SimpleModuleDefinition remoteDefinition) {
		ModuleDefinition currentTargetDefinition = currentLocalDefinition(remoteDefinition);
		SimpleModuleDefinition targetDefinition = (SimpleModuleDefinition) currentTargetDefinition;
		if (staleness.isStale(targetDefinition, remoteDefinition)) {
			InputStream is = null;
			try {
				is = resolver.getResource(remoteDefinition.getLocation()).getInputStream();
			}
			catch (IOException e) {
				throw new RuntimeIOException("Error while copying module", e);
			}
			localRegistry.delete(remoteDefinition);
			UploadedModuleDefinition definitionToInstall = new UploadedModuleDefinition(remoteDefinition.getName(), remoteDefinition.getType(), is);
			localRegistry.registerNew(definitionToInstall);
		}
	}

	/**
	 * Returns the current version corresponding to a given module definition, as seen by the local registry.
	 */
	private ModuleDefinition currentLocalDefinition(ModuleDefinition remoteDefinition) {
		return localRegistry.findDefinition(remoteDefinition.getName(), remoteDefinition.getType());
	}


	@Override
	public boolean delete(ModuleDefinition definition) {
		return remoteRegistry.delete(definition);
	}

	@Override
	public boolean registerNew(ModuleDefinition definition) {
		return remoteRegistry.registerNew(definition);
	}
}
