/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.shell.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * Quick and dirty class that generates asciidoc snippets for each module's options.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsReferenceDoc {

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Autowired
	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	private void run(String root) throws IOException {
		File rootFile = new File(root);
		Assert.isTrue(rootFile.exists() && rootFile.isDirectory() || rootFile.mkdirs(), rootFile.getAbsolutePath()
				+ " does not exist or could not be created");
		for (ModuleType moduleType : ModuleType.values()) {
			for (ModuleDefinition def : moduleRegistry.findDefinitions(moduleType)) {
				ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(def);
				File file = new File(rootFile, String.format("%s.%s.asciidoc", def.getType(), def.getName()));
				generateAsciidoc(file, def, moduleOptionsMetadata);
			}
		}
	}

	private void generateAsciidoc(File file, ModuleDefinition def, ModuleOptionsMetadata moduleOptionsMetadata)
			throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(file));
		//		PrintStream out = System.out;

		out.format("The **%s** %s has the following options:%n%n", def.getName(), def.getType());
		List<ModuleOption> options = new ArrayList<ModuleOption>();
		for (ModuleOption mo : moduleOptionsMetadata) {
			options.add(mo);
		}
		Collections.sort(options, new Comparator<ModuleOption>() {

			@Override
			public int compare(ModuleOption o1, ModuleOption o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (ModuleOption mo : options) {
			String prettyDefault = prettyfyDefaultValue(mo);
			String maybeEnumHint = maybeEnumHint(mo);
			out.format("%s:: %s *(%s, %s%s)*%n", mo.getName(), mo.getDescription(), mo.getType().getSimpleName(),
					prettyDefault, maybeEnumHint);
		}
		if (out == System.out) {
			out.println();
		}
		else {
			out.flush();
			out.close();
		}
	}

	/**
	 * When the type of an option is an enum, document all possible values
	 */
	private String maybeEnumHint(ModuleOption mo) {
		if (Enum.class.isAssignableFrom(mo.getType())) {
			String values = StringUtils.arrayToCommaDelimitedString(mo.getType().getEnumConstants());
			return String.format(", possible values: `%s`", values);
		}
		else
			return "";
	}

	private String prettyfyDefaultValue(ModuleOption mo) {
		String result = mo.getDefaultValue() == null ? "no default" : String.format("default: `%s`",
				mo.getDefaultValue());
		result = result.replace(ModulePlaceholders.XD_STREAM_NAME, "<stream name>");
		result = result.replace(ModulePlaceholders.XD_JOB_NAME, "<job name>");
		return result;
	}

	public static void main(String[] args) throws IOException {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ModuleOptionsReferenceDoc.Config.class);

		ModuleOptionsReferenceDoc runner = applicationContext.getBean(ModuleOptionsReferenceDoc.class);
		Assert.isTrue(args.length == 1, "Need to pass in root dir where files will be generated");
		runner.run(args[0]);
		applicationContext.close();
	}

	@Configuration
	public static class Config {

		@Autowired
		public void setEnvironment(Environment environment) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("xd.config.home", "file:../config");
			((ConfigurableEnvironment) environment).getPropertySources().addFirst(new MapPropertySource("foo", map));
		}

		@Bean
		public ModuleRegistry moduleRegistry() {
			return new ResourceModuleRegistry("file:../modules");
		}

		@Bean
		public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
			return new DefaultModuleOptionsMetadataResolver();
		}

		@Bean
		public ModuleOptionsReferenceDoc runner() {
			return new ModuleOptionsReferenceDoc();
		}

	}


}
