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
 * A ModuleRegistry that is configured with two delegates: a source and a target registry. This registry will return
 * results that exist in the source registry but will synchronize them with the target registry. Results returned are always
 * from the <em>target</em> registry (unless composed). Mutative operations go through to the source repository though.
 *
 * <p>This is useful as reading
 * Boot uber-jars requires local {@code java.io.File} access, but modules may reside in a remote registry (<i>e.g.</i>
 * backed by HDFS). For such a case, simply use this registry as the main registry, configuring it with the {@code hdfs://} registry as
 * its source and the {@code file://} one as its target.</p>
 *
 * @author Eric Bottard
 */
public class SynchronizingModuleRegistry implements WritableModuleRegistry {

	private final WritableModuleRegistry sourceRegistry;

	private final WritableModuleRegistry targetRegistry;

	private final StalenessCheck staleness = new MD5StalenessCheck();

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public SynchronizingModuleRegistry(WritableModuleRegistry sourceRegistry, WritableModuleRegistry targetRegistry) {
		Assert.notNull(sourceRegistry, "sourceRegistry cannot be null");
		Assert.notNull(targetRegistry, "targetRegistry cannot be null");
		this.sourceRegistry = sourceRegistry;
		this.targetRegistry = targetRegistry;
	}


	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		ModuleDefinition remoteDefinition = sourceRegistry.findDefinition(name, moduleType);
		if (remoteDefinition == null || remoteDefinition.isComposed()) {
			return remoteDefinition;
		}

		copyIfStale(remoteDefinition);
		return currentTargetDefinition(remoteDefinition);
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		List<ModuleDefinition> remoteDefinitions = sourceRegistry.findDefinitions(name);
		return refresh(remoteDefinitions);
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		List<ModuleDefinition> remoteDefinitions = sourceRegistry.findDefinitions(type);
		return refresh(remoteDefinitions);
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		List<ModuleDefinition> remoteDefinitions = sourceRegistry.findDefinitions();
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
				copyIfStale(remoteDefinition);
				result.add(currentTargetDefinition(remoteDefinition));
			}
		}

		return result;
	}


	/**
	 * Checks whether the target version of the definition is stale, and if it is, triggers an update (as a deletion then
	 * new registration).
	 */
	private synchronized void copyIfStale(ModuleDefinition remoteDefinition) {
		SimpleModuleDefinition simpleRemoteDefinition = (SimpleModuleDefinition) remoteDefinition;
		ModuleDefinition currentTargetDefinition = currentTargetDefinition(remoteDefinition);
		SimpleModuleDefinition targetDefinition = (SimpleModuleDefinition) currentTargetDefinition;
		if (staleness.isStale(targetDefinition, simpleRemoteDefinition)) {
			InputStream is = null;
			try {
				is = resolver.getResource(simpleRemoteDefinition.getLocation()).getInputStream();
			}
			catch (IOException e) {
				throw new RuntimeIOException("Error while copying module", e);
			}
			UploadedModuleDefinition vehicle = new UploadedModuleDefinition(remoteDefinition.getName(), remoteDefinition.getType(), is);
			targetRegistry.delete(vehicle);
			targetRegistry.registerNew(vehicle);
		}
	}

	/**
	 * Returns the current version corresponding to a given module definition, as seen by the target registry.
	 */
	private ModuleDefinition currentTargetDefinition(ModuleDefinition remoteDefinition) {
		return targetRegistry.findDefinition(remoteDefinition.getName(), remoteDefinition.getType());
	}


	@Override
	public boolean delete(ModuleDefinition definition) {
		return sourceRegistry.delete(definition);
	}

	@Override
	public boolean registerNew(ModuleDefinition definition) {
		return sourceRegistry.registerNew(definition);
	}
}
