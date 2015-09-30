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

package org.springframework.xd.documentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleUtils;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * A class that generates asciidoc snippets for each module's options.
 *
 * <p>For each file passed as an argument, will replace parts of the file (inplace) in between {@code //^<type>.<name>}
 * and {@code //$<type>.<name>} with a generated snippet documenting options. Those start and end fences are copied as-is,
 * so that a subsequent run regenerates uptodate doco.
 * </p>
 *
 * @author Eric Bottard
 */
public class ModuleOptionsReferenceDoc {

	/**
	 * Matches "//^<type>.<name>" exactly.
	 */
	private static final Pattern FENCE_START_REGEX = Pattern.compile("^//\\^([^.]+)\\.([^.]+)$");

	private ModuleRegistry moduleRegistry = new ResourceModuleRegistry("file:./modules");

	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver = new DefaultModuleOptionsMetadataResolver();

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	public static void main(String... paths) throws IOException {
		ModuleOptionsReferenceDoc runner = new ModuleOptionsReferenceDoc();
		for (String path : paths) {
			runner.updateSingleFile(path);
		}
	}

	private void updateSingleFile(String path) throws IOException {
		File originalFile = new File(path);
		Assert.isTrue(originalFile.exists() && !originalFile.isDirectory(),
				String.format("'%s' does not exist or points to a directory", originalFile.getAbsolutePath()));

		File backup = new File(originalFile.getAbsolutePath() + ".backup");
		originalFile.renameTo(backup);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(backup), "UTF-8"));

		PrintStream out = new PrintStream(new FileOutputStream(originalFile), false, "UTF-8");

		ModuleType type = null;
		String name = null;
		int openingLineNumber = 0;
		int ln = 1;
		for (String line = reader.readLine(); line != null; line = reader.readLine(), ln++) {
			Matcher startMatcher = FENCE_START_REGEX.matcher(line);
			if (startMatcher.matches()) {
				checkPreviousTagHasBeenClosed(originalFile, backup, out, type, name, openingLineNumber);
				type = ModuleType.valueOf(startMatcher.group(1));
				name = startMatcher.group(2);
				openingLineNumber = ln;
				out.println(line);
			}
			else if (type != null && line.equals(String.format("//$%s.%s", type, name))) {
				generateWarning(out, name, type);
				generateAsciidoc(out, name, type);
				type = null;
				name = null;
				out.println(line);
			}
			else if (type == null) {
				out.println(line);
			}
		}
		checkPreviousTagHasBeenClosed(originalFile, backup, out, type, name, openingLineNumber);

		out.close();
		reader.close();

		backup.delete();

	}

	private void checkPreviousTagHasBeenClosed(File originalFile, File backup, PrintStream out, ModuleType type,
			String name, int openingLineNumber) {
		if (type != null) {
			out.close();
			originalFile.delete();
			backup.renameTo(originalFile);
			throw new IllegalStateException(String.format(
					"In %s, found '//^%s.%s' @line %d with no matching '//$%2$s.%3$s'",
					originalFile.getAbsolutePath(), type, name, openingLineNumber));
		}
	}

	private void generateWarning(PrintStream out, String name, ModuleType type) {
		out.format("// DO NOT MODIFY THE LINES BELOW UNTIL THE CLOSING '//$%s.%s' TAG%n", type, name);
		out.format("// THIS SNIPPET HAS BEEN GENERATED BY %s AND MANUAL EDITS WILL BE LOST%n",
				ModuleOptionsReferenceDoc.class.getSimpleName());
	}

	private void generateAsciidoc(PrintStream out, String name, ModuleType type)
			throws IOException {
		ModuleDefinition def = moduleRegistry.findDefinition(name, type);
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(def);

		Resource moduleLoc = resourcePatternResolver.getResource(((SimpleModuleDefinition) def).getLocation());
		ClassLoader moduleClassLoader = ModuleUtils.createModuleDiscoveryClassLoader(moduleLoc, ModuleOptionsReferenceDoc.class.getClassLoader());
		if (!moduleOptionsMetadata.iterator().hasNext()) {
			out.format("The **%s** %s has no particular option (in addition to options shared by all modules)%n%n",
					pt(def.getName()), pt(def.getType()));
			return;
		}

		out.format("The **%s** %s has the following options:%n%n", pt(def.getName()), pt(def.getType()));
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
			String prettyDefault = prettifyDefaultValue(mo);
			String maybeEnumHint = generateEnumValues(mo, moduleClassLoader);
			out.format("%s:: %s *(%s, %s%s)*%n", pt(mo.getName()), pt(mo.getDescription()),
					pt(shortClassName(mo.getType())),
					prettyDefault, maybeEnumHint);
		}
	}

	private String shortClassName(String fqName) {
		int lastDot = fqName.lastIndexOf('.');
		return lastDot >= 0 ? fqName.substring(lastDot + 1) : fqName;
	}


	/**
	 * When the type of an option is an enum, document all possible values
	 */
	private String generateEnumValues(ModuleOption mo, ClassLoader moduleClassLoader) {
		// Attempt to convert back to com.acme.Foo$Bar form
		String canonical = mo.getType();
		String system = canonical.replaceAll("(.*\\p{Upper}[^\\.]*)\\.(\\p{Upper}.*)", "$1\\$$2");
		Class<?> clazz = null;
		try {
			clazz = Class.forName(system, false, moduleClassLoader);
		}
		catch (ClassNotFoundException e) {
			return "";
		}
		if (Enum.class.isAssignableFrom(clazz)) {
			String values = StringUtils.arrayToCommaDelimitedString(clazz.getEnumConstants());
			return String.format(", possible values: `%s`", values);
		}
		else
			return "";
	}

	private String prettifyDefaultValue(ModuleOption mo) {
		if (mo.getDefaultValue() == null) {
			return "no default";
		}
		String result = stringify(mo.getDefaultValue());
		result = result.replace(ModulePlaceholders.XD_STREAM_NAME, "<stream name>");
		result = result.replace(ModulePlaceholders.XD_JOB_NAME, "<job name>");
		return "default: `" + result + "`";
	}

	private String stringify(Object element) {
		Class<?> clazz = element.getClass();
		if (clazz == byte[].class) {
			return Arrays.toString((byte[]) element);
		}
		else if (clazz == short[].class) {
			return Arrays.toString((short[]) element);
		}
		else if (clazz == int[].class) {
			return Arrays.toString((int[]) element);
		}
		else if (clazz == long[].class) {
			return Arrays.toString((long[]) element);
		}
		else if (clazz == char[].class) {
			return Arrays.toString((char[]) element);
		}
		else if (clazz == float[].class) {
			return Arrays.toString((float[]) element);
		}
		else if (clazz == double[].class) {
			return Arrays.toString((double[]) element);
		}
		else if (clazz == boolean[].class) {
			return Arrays.toString((boolean[]) element);
		}
		else if (element instanceof Object[]) {
			return Arrays.deepToString((Object[]) element);
		}
		else {
			return element.toString();
		}
	}

	/**
	 * Return an asciidoc passthrough version of some text, in case the original text contains characters
	 * that would be (mis)interpreted by asciidoc.
	 */
	private String pt(Object original) {
		return "$$" + original + "$$";
	}


}
