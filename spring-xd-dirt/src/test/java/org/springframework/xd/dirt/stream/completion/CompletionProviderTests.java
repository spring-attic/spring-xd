/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream.completion;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.rest.client.domain.CompletionKind.job;
import static org.springframework.xd.rest.client.domain.CompletionKind.module;
import static org.springframework.xd.rest.client.domain.CompletionKind.stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleDefinitionRepository;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleDependencyRepository;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { CompletionProviderTests.Config.class })
public class CompletionProviderTests {

	@Autowired
	private CompletionProvider completionProvider;

	@Autowired
	private ModuleDefinitionRepository moduleDefinitionRepository;

	@Test
	// <TAB> => file,http,etc
	public void testEmptyStartShouldProposeSourceModules() {
		List<String> completions = completionProvider.complete(stream, "");

		assertThat(new HashSet<>(completions), equalTo(new HashSet<>(namesOfModulesWithType(ModuleType.source))));
	}

	@Test
	// fi<TAB> => file
	public void testUnfinishedModuleNameShouldReturnCompletions() {
		List<String> completions = completionProvider.complete(stream, "fi");
		assertThat(new HashSet<>(completions), hasItem(startsWith("file")));

		completions = completionProvider.complete(stream, "file | tr");
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | transform")));
	}

	@Test
	// file | filter<TAB> => file | filter | foo, etc
	public void testValidSubStreamDefinitionShouldReturnPipe() {
		List<String> completions = completionProvider.complete(stream, "file | filter");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter |")));
	}

	@Test
	// file | filter<TAB> => file | filter --foo=, etc
	public void testValidSubStreamDefinitionShouldReturnModuleOptions() {
		List<String> completions = completionProvider.complete(stream, "file | filter");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
	}

	@Test
	// file | filter -<TAB> => file | filter --foo,etc
	public void testOneDashShouldReturnTwoDashes() {
		List<String> completions = completionProvider.complete(stream, "file | filter -");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file | filter --<TAB> => file | filter --foo,etc
	public void testTwoDashesShouldReturnOptions() {
		List<String> completions = completionProvider.complete(stream, "file | filter --");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file |<TAB> => file | foo,etc
	public void testDanglingPipeShouldReturnExtraModulesOnlyOneModule() {
		List<String> completions = completionProvider.complete(stream, "file |");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | script")));
	}

	@Test
	// file |<TAB> => file | filter | foo,etc
	// Adding a specified test for this because of the way the parser guesses module type
	// may currently interfere if there is only one module (thinks it's a job)
	public void testDanglingPipeShouldReturnExtraModulesMoreThanOneModule() {
		List<String> completions = completionProvider.complete(stream, "file | filter |");

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter | filter")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter | script")));
	}

	@Test
	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	public void testUnfinishedOptionNameShouldComplete() {
		List<String> completions = completionProvider.complete(stream, "file | filter | jdbc --url=foo --ini");

		assertThat(new HashSet<>(completions),
				hasItem(startsWith("file | filter | jdbc --url=foo --initializerScript")));
		assertThat(new HashSet<>(completions),
				hasItem(startsWith("file | filter | jdbc --url=foo --initializeDatabase")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter | jdbc --url=foo --driverClass"))));

		completions = completionProvider.complete(stream, "file | filter --ex");
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file | counter --name=foo --inputType=bar<TAB> => we're done
	public void testSinkWithAllOptionsSetCantGoFurther() {
		List<String> completions = completionProvider.complete(stream,
				"file | counter --name=foo --inputType=text/plain");

		assertThat(completions, hasSize(0));
	}

	@Test
	// file | counter --name=<TAB> => nothing
	public void testInGenericOptionValueCantProposeAnything() {
		List<String> completions = completionProvider.complete(stream,
				"file | counter --name=");

		assertThat(completions, hasSize(0));
	}

	@Test
	@Ignore("XD-1284")
	// file | file --binary=<TAB> => we know it's a closed set of values
	public void testInOptionValueBooleanNoStartAtAll() {
		List<String> completions = completionProvider.complete(stream,
				"file | file --binary=");
		assertThat(completions, hasItem(startsWith("file | file --binary=true")));
		assertThat(completions, hasItem(startsWith("file | file --binary=false")));
	}

	@Test
	@Ignore("XD-1284")
	// file | file --binary=t<TAB> => we know it's a closed set, and 'true' matches
	public void testInOptionValueBooleanValidStart() {
		List<String> completions = completionProvider.complete(stream,
				"file | file --binary=t");
		assertThat(completions, hasItem(startsWith("file | file --binary=true")));
	}

	@Test
	@Ignore("XD-1284")
	// file | file --binary=foo<TAB> => we know it's wrong, so return nothing
	public void testInOptionValueBooleanInvalidStart() {
		List<String> completions = completionProvider.complete(stream,
				"file | file --binary=foo");
		assertThat(completions, hasSize(0));
	}

	@Test
	@Ignore("XD-1284")
	// file | hdfs --codec=<TAB> // same logic as testInOptionValueBoolean
	public void testInOptionValueEnumNoStartAtAll() {
		List<String> completions = completionProvider.complete(stream,
				"file | hdfs --codec=");
		assertThat(completions, hasItem(startsWith("hdfs --codec=SNAPPY")));
		assertThat(completions, hasItem(startsWith("hdfs --codec=BZIP2")));
	}

	@Test
	@Ignore("XD-1284")
	// file | hdfs --codec=S<TAB> => SNAPPY // same logic as testInOptionValueBoolean
	public void testInOptionValueEnumValidStart() {
		List<String> completions = completionProvider.complete(stream,
				"hdfs --codec=S");
		assertThat(completions, hasItem(startsWith("hdfs --codec=SNAPPY")));
	}

	@Test
	@Ignore("XD-1284")
	// file | hdfs --codec=FOOBAR<TAB> // same logic as testInOptionValueBoolean
	public void testInOptionValueEnumInvalidStart() {
		List<String> completions = completionProvider.complete(stream,
				"hdfs --codec=FOOBAR");
		assertThat(completions, hasSize(0));
	}

	@Test
	public void testJobNameCompletions() {
		List<String> completions = completionProvider.complete(job, "");
		assertThat(completions, hasItem(startsWith("hdfs")));
		assertThat(completions, not(hasItem(startsWith("gemfire-cq"))));

		completions = completionProvider.complete(job, "hdf");
		assertThat(completions, hasItem(startsWith("hdfs")));

	}

	@Test
	public void testJobOptionsCompletions() {
		List<String> completions = completionProvider.complete(job, "filejdbc --");
		assertThat(completions, hasItem(startsWith("filejdbc --resources")));

	}

	@Test
	public void testComposedModuleCompletions() {
		List<String> completions = completionProvider.complete(module, "");
		assertThat(completions, hasItem(startsWith("http")));
		assertThat(completions, hasItem(startsWith("transform")));
		assertThat(completions, not(hasItem(startsWith("splunk"))));

		completions = completionProvider.complete(module, "t");
		assertThat(completions, hasItem(startsWith("tcp")));
		assertThat(completions, hasItem(startsWith("transform")));

		completions = completionProvider.complete(module, "tcp | s");
		assertThat(completions, hasItem(startsWith("tcp | splitter")));
		assertThat(completions, hasItem(startsWith("tcp | splunk")));
		assertThat(completions, not(hasItem(startsWith("tcp | syslog-tcp"))));

		completions = completionProvider.complete(module, "transform | s");
		assertThat(completions, hasItem(startsWith("transform | splitter")));
		assertThat(completions, hasItem(startsWith("transform | splunk")));
		assertThat(completions, not(hasItem(startsWith("transform | syslog-tcp"))));
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
	@ComponentScan(basePackageClasses = CompletionProvider.class)
	public static class Config {

		@Bean
		public ModuleRegistry moduleRegistry() {
			return new ResourceModuleRegistry("file:../modules");
		}

		@Bean
		public ModuleDependencyRepository moduleDependencyRepository() {
			return new ZooKeeperModuleDependencyRepository(zooKeeperConnection());
		}

		@Bean
		public ModuleDefinitionRepository moduleDefinitionRepository() {
			return new ZooKeeperModuleDefinitionRepository(moduleRegistry(), moduleDependencyRepository(),
					zooKeeperConnection());
		}

		@Bean
		public XDParser parser(ModuleDefinitionRepository moduleDefinitionRepository,
				ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
			return new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
		}

		@Bean
		public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
			return new DefaultModuleOptionsMetadataResolver();
		}

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		}

		@Bean
		public EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}
	}

}
