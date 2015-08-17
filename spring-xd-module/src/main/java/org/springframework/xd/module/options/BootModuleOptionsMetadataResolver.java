/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.module.options;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * A ModuleOptionsMetadataResolver that knows how to read Spring Boot metadata for configuration properties
 * and exposes some of the properties as module options.
 *
 * @author Eric Bottard
 */
public class BootModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver {


	private ResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();

	/**
	 * The prefix with which configuration options must start to be considered module options.
	 *
	 * <p>The prefix-less form will be used for module options, while at runtime,
	 * the module may expect the long form.</p>
	 */
	private String prefix = "module.";


	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
		if (moduleDefinition.isComposed()) {
			return null;
		}
		SimpleModuleDefinition simpleModuleDefinition = (SimpleModuleDefinition) moduleDefinition;
		Resource resource = resourceLoader.getResource(simpleModuleDefinition.getLocation());
		ClassLoader moduleClassLoader = ModuleUtils.createModuleDiscoveryClassLoader(resource, BootModuleOptionsMetadataResolver.class.getClassLoader());

		ResourcePatternResolver moduleResourceLoader = new PathMatchingResourcePatternResolver(moduleClassLoader);

		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();

		try {
			for (Resource r : moduleResourceLoader.getResources("classpath*:/META-INF/*spring-configuration-metadata.json")) {
				builder.withJsonResource(r.getInputStream());
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		ModuleUtils.closeClassLoader(moduleClassLoader);

		ConfigurationMetadataRepository repo = builder.build();
		SimpleModuleOptionsMetadata result = new SimpleModuleOptionsMetadata();

		for (Map.Entry<String, ConfigurationMetadataProperty> kv : repo.getAllProperties().entrySet()) {
			String key = kv.getKey();
			if (!key.startsWith(prefix)) {
				continue;
			}
			ConfigurationMetadataProperty value = kv.getValue();
			ModuleOption mo = new ModuleOption(key.substring(prefix.length()), value.getDescription())
				.withDefaultValue(value.getDefaultValue())
				.withType(value.getType());
			result.add(mo);

		}

		return result;
	}


}
