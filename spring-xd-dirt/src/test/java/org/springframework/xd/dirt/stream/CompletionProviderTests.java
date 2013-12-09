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

package org.springframework.xd.dirt.stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.rest.client.domain.CompletionKind.stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDefinitionRepository;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDependencyRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { CompletionProviderTests.Config.class })
public class CompletionProviderTests {

	@Autowired
	private CompletionProvider completionProvider;

	@Autowired
	private ModuleDefinitionRepository moduleDefinitionRepository;

	@Test
	public void testEmptyStartShouldProposeSourceModules() {
		List<String> completions = completionProvider.complete(stream, "");

		assertThat(new HashSet<>(completions), equalTo(new HashSet<>(namesOfModulesWithType(ModuleType.source))));
	}

	@Test
	public void testUnfinishedModuleNameShouldReturnCommletions() {
		List<String> completions = completionProvider.complete(stream, "fi");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file")));
	}

	@Test
	public void testValidSubStreamDefinitionShouldReturnPipe() {
		List<String> completions = completionProvider.complete(stream, "file | filter");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter |")));
	}

	@Test
	public void testValidSubStreamDefinitionShouldReturnModuleOptions() {
		List<String> completions = completionProvider.complete(stream, "file | filter");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
	}

	@Test
	public void testOneDashShouldReturnTwoDashes() {
		List<String> completions = completionProvider.complete(stream, "file | filter -");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	private List<String> namesOfModulesWithType(ModuleType type) {
		Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
		List<String> result = new ArrayList<String>();
		for (ModuleDefinition mod : mods) {
			result.add(mod.getName());
		}
		return result;
	}

	@Configuration
	public static class Config {

		@Bean
		public ModuleRegistry moduleRegistry() {
			return new ResourceModuleRegistry("file:../modules");
		}

		@Bean
		public ModuleDependencyRepository moduleDependencyRepository() {
			return new InMemoryModuleDependencyRepository();
		}

		@Bean
		public ModuleDefinitionRepository moduleDefinitionRepository() {
			return new InMemoryModuleDefinitionRepository(moduleRegistry(), moduleDependencyRepository());
		}

		@Bean
		public XDParser parser(ModuleDefinitionRepository moduleDefinitionRepository) {
			return new XDStreamParser(moduleDefinitionRepository);
		}

		@Bean
		public CompletionProvider completionProvider(XDParser parser,
				ModuleDefinitionRepository moduleDefinitionRepository) {
			return new CompletionProvider(parser, moduleDefinitionRepository);
		}

	}

}
