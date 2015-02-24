/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.server.options;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.NamedOptionDef;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * An {@link OptionHandler} that scans a resource pattern for existing resources (using a single '*' wildcard) and
 * allows all String values <i>s</i> that would fit if that single wildcard was replaced by <i>s</i>.
 * 
 * <p>
 * Given that an option handler has to appear as an annotation parameter, expected usage is to sublcass this class,
 * provide the canonical 3 arg constructor to {@link OptionHandler} and pass the resource pattern as the 4th argument.
 * </p>
 * 
 * @author Eric Bottard
 */
public abstract class ResourcePatternScanningOptionHandler extends OptionHandler<String> {

	protected final Set<String> possibleValues = new HashSet<String>();

	private final Set<String> excluded = new HashSet<String>();

	protected ResourcePatternScanningOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter,
			String glob)
			throws IOException {
		super(parser, option, setter);
		init(glob);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		String s = params.getParameter(0);
		if (!possibleValues.contains(s)) {
			String errorMessage = String.format("'%s' is not a valid value. Possible values are %s", s, possibleValues);
			if (this.option instanceof NamedOptionDef) {
				NamedOptionDef named = (NamedOptionDef) this.option;
				errorMessage = String.format("'%s' is not a valid value for option %s. Possible values are %s", s,
						named.name(), possibleValues);
			}
			if (excluded.contains(s)) {
				errorMessage += String.format(
						". Note that '%s' has been explicitly excluded from the list of possible values,"
								+ " even though a resource with that name exists", s);
			}
			throw new CmdLineException(owner, errorMessage);
		}
		setter.addValue(s);
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return possibleValues.toString().replace(",", " |");
	}

	private void init(String glob, String... excludes) throws IOException {
		String resolved = CommandLinePropertySourceOverridingListener.getCurrentEnvironment().resolvePlaceholders(glob);
		int protocolColon = resolved.indexOf(':');
		String withoutProtocol = protocolColon != -1 ? resolved.substring(protocolColon + 1) : resolved;
		Pattern capturing = Pattern.compile(".*" + withoutProtocol.replace("*", "([^/]*)") + ".*");

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		for (Resource r : resolver.getResources(resolved)) {
			if (!shouldConsider(r)) {
				continue;
			}
			String path = r.getURL().toString();
			Matcher matcher = capturing.matcher(path);
			if (!matcher.matches()) {
				throw new IllegalStateException(
						String.format("Expected to match '%s' with regex '%s'", path, capturing));
			}
			possibleValues.add(matcher.group(1));
		}

	}

	/**
	 * Whether the matched Spring resource should even be considered for inclusion in the result set.
	 * Default implementation just returns true. 
	 */
	protected boolean shouldConsider(Resource r) {
		return true;
	}

	protected void exclude(String... excludes) {
		excluded.addAll(Arrays.asList(excludes));
		possibleValues.removeAll(excluded);
	}

	protected void include(String... includes) {
		possibleValues.addAll(Arrays.asList(includes));
	}

}
