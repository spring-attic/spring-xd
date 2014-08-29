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

package org.springframework.xd.dirt.stream.completionnoscan;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.xd.rest.domain.CompletionKind.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleDependencyRepository;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.stream.completion.CompletionProvider;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CompletionProviderTests.Config.class})
public class CompletionProviderTests {

	@Autowired
	private CompletionProvider completionProvider;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Test
	// <TAB> => file,http,etc
	public void testEmptyStartShouldProposeSourceModules() {
		List<String> completions = completionProvider.complete(stream, "", 1);

		assertThat(new HashSet<>(completions), equalTo(new HashSet<>(namesOfModulesWithType(ModuleType.source))));
	}

	@Test
	// fi<TAB> => file
	public void testUnfinishedModuleNameShouldReturnCompletions() {
		List<String> completions = completionProvider.complete(stream, "fi", 1);
		assertThat(new HashSet<>(completions), hasItem(startsWith("file")));

		completions = completionProvider.complete(stream, "file | tr", 1);
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | transform")));
	}

	@Test
	// file | filter<TAB> => file | filter | foo, etc
	public void testValidSubStreamDefinitionShouldReturnPipe() {
		List<String> completions = completionProvider.complete(stream, "file | filter", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter |")));
	}

	@Test
	// file | filter<TAB> => file | filter --foo=, etc
	public void testValidSubStreamDefinitionShouldReturnModuleOptions() {
		List<String> completions = completionProvider.complete(stream, "file | filter", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
	}

	@Test
	// file | filter -<TAB> => file | filter --foo,etc
	public void testOneDashShouldReturnTwoDashes() {
		List<String> completions = completionProvider.complete(stream, "file | filter -", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file | filter --<TAB> => file | filter --foo,etc
	public void testTwoDashesShouldReturnOptions() {
		List<String> completions = completionProvider.complete(stream, "file | filter --", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --script=")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter --expression=")));
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file |<TAB> => file | foo,etc
	public void testDanglingPipeShouldReturnExtraModulesOnlyOneModule() {
		List<String> completions = completionProvider.complete(stream, "file |", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | script")));
	}

	@Test
	// file |<TAB> => file | filter | foo,etc
	// Adding a specified test for this because of the way the parser guesses module type
	// may currently interfere if there is only one module (thinks it's a job)
	public void testDanglingPipeShouldReturnExtraModulesMoreThanOneModule() {
		List<String> completions = completionProvider.complete(stream, "file | filter |", 1);

		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter | filter")));
		assertThat(new HashSet<>(completions), hasItem(startsWith("file | filter | script")));
	}

	@Test
	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	public void testUnfinishedOptionNameShouldComplete() {
		List<String> completions = completionProvider.complete(stream, "file | filter | jdbc --url=foo --ini", 1);

		assertThat(new HashSet<>(completions),
				hasItem(startsWith("file | filter | jdbc --url=foo --initializerScript")));
		assertThat(new HashSet<>(completions),
				hasItem(startsWith("file | filter | jdbc --url=foo --initializeDatabase")));
		assertThat(new HashSet<>(completions),
				not(hasItem(startsWith("file | filter | jdbc --url=foo --driverClassName"))));

		completions = completionProvider.complete(stream, "file | filter --ex", 1);
		assertThat(new HashSet<>(completions), not(hasItem(startsWith("file | filter |"))));
	}

	@Test
	// file | counter --name=foo --inputType=bar<TAB> => we're done
	public void testSinkWithAllOptionsSetCantGoFurther() {
		List<String> completions = completionProvider.complete(stream,
				"file | log --expression=payload --level=INFO --name=foo --inputType=text/plain", 1);

		assertThat(completions, hasSize(0));
	}

	@Test
	// file | counter --name=<TAB> => nothing
	public void testInGenericOptionValueCantProposeAnything() {
		List<String> completions = completionProvider.complete(stream,
				"file | counter --name=", 1);

		assertThat(completions, hasSize(0));
	}

	@Test
	public void testJobNameCompletions() {
		List<String> completions = completionProvider.complete(job, "", 1);
		assertThat(completions, hasItem(startsWith("hdfs")));
		assertThat(completions, not(hasItem(startsWith("gemfire-cq"))));

		completions = completionProvider.complete(job, "hdf", 1);
		assertThat(completions, hasItem(startsWith("hdfs")));

	}

	@Test
	public void testJobOptionsCompletions() {
		List<String> completions = completionProvider.complete(job, "filejdbc --", 1);
		assertThat(completions, hasItem(startsWith("filejdbc --resources")));

	}

	@Test
	public void testComposedModuleCompletions() {
		List<String> completions = completionProvider.complete(module, "", 1);
		assertThat(completions, hasItem(startsWith("http")));
		assertThat(completions, hasItem(startsWith("transform")));
		assertThat(completions, not(hasItem(startsWith("splunk"))));

		completions = completionProvider.complete(module, "t", 1);
		assertThat(completions, hasItem(startsWith("tcp")));
		assertThat(completions, hasItem(startsWith("transform")));

		completions = completionProvider.complete(module, "tcp | s", 1);
		assertThat(completions, hasItem(startsWith("tcp | splitter")));
		assertThat(completions, hasItem(startsWith("tcp | splunk")));
		assertThat(completions, not(hasItem(startsWith("tcp | syslog-tcp"))));

		completions = completionProvider.complete(module, "transform | s", 1);
		assertThat(completions, hasItem(startsWith("transform | splitter")));
		assertThat(completions, hasItem(startsWith("transform | splunk")));
		assertThat(completions, not(hasItem(startsWith("transform | syslog-tcp"))));
	}

	@Test
	// queue:foo > <TAB>  ==> add module names
	public void testXD1706() {
		List<String> completions = completionProvider.complete(stream, "queue:foo > ", 1);
		assertThat(completions, hasItem(startsWith("queue:foo > http-client")));
		assertThat(completions, hasItem(startsWith("queue:foo > splunk")));
		assertThat(completions, not(hasItem(startsWith("queue:foo > twittersearch"))));
	}

	@Test
	// tap:stream:foo > <TAB>  ==> add module names
	public void testXD1706Variant() {
		List<String> completions = completionProvider.complete(stream, "tap:stream:foo > ", 1);
		assertThat(completions, hasItem(startsWith("tap:stream:foo > http-client")));
		assertThat(completions, hasItem(startsWith("tap:stream:foo > splunk")));
		assertThat(completions, not(hasItem(startsWith("tap:stream:foo > twittersearch"))));
	}

	@Test
	public void testCompleteAlreadyStartedNameAfterChannel() {
		List<String> completions = completionProvider.complete(stream, "queue:foo > s", 1);
		// XD-1830: Currently does not support adding processors due do the way
		// module type guessing works
		//assertThat(completions, hasItem(startsWith("queue:foo > splitter")));
		assertThat(completions, hasItem(startsWith("queue:foo > splunk")));
		assertThat(completions, not(hasItem(startsWith("queue:foo > syslog"))));
	}

	@Test
	public void testHiddenOptionNames() {
		List<String> completions = completionProvider.complete(stream, "http | jdbc ", 1);
		assertThat(completions, not(hasItem(startsWith("http | jdbc --maxIdle"))));

		completions = completionProvider.complete(stream, "http | jdbc ", 2);
		assertThat(completions, hasItem(startsWith("http | jdbc --maxIdle")));

	}

	private List<String> namesOfModulesWithType(ModuleType type) {
		List<ModuleDefinition> mods = moduleRegistry.findDefinitions(type);
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
		public XDParser parser() {
			return new XDStreamParser(moduleRegistry(), moduleOptionsMetadataResolver());
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
