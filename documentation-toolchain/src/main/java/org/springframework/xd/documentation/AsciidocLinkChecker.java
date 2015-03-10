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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


/**
 * A tool that checks that for files that will form the whole "concatenated" documentation, any link of the form
 * <pre>
 * {@code
 * link:<file>:<anchor>[<label>]
 * }
 * </pre>
 * indeed points to an <em>explicit</em> and <em>unique across all included files</em> anchor in the given file.
 *
 * @author Eric Bottard
 */
public class AsciidocLinkChecker {

	private Map<String, String> anchors = new HashMap<String, String>();

	private static final Pattern ANCHOR_PATTERN = Pattern.compile("^\\[\\[([a-zA-Z_0-9\\-]+)\\]\\]");

	private static final Pattern LINK_PATTERN = Pattern.compile("xref:([a-zA-Z_0-9\\-]+)#([a-zA-Z_0-9\\-]+)");

	private Set<Link> links = new HashSet<AsciidocLinkChecker.Link>();

	private StringBuilder errors = new StringBuilder();;

	private int nbErrors = 0;

	private String rootPath;


	public AsciidocLinkChecker(String rootPath) {
		this.rootPath = rootPath;
	}

	public static void main(String[] args) throws Exception {
		AsciidocLinkChecker checker = new AsciidocLinkChecker(args[0]);
		checker.check();
		if (checker.nbErrors > 0) {
			throw new AssertionError(String.format("There were %d errors.%n%s", checker.nbErrors, checker.errors));
		}
	}

	private void check() throws IOException, UnsupportedEncodingException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		for (Resource resource : resolver.getResources(rootPath)) {
			String current = stripExtension(resource.getFilename());
			BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"));
			String line = null;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;

				Matcher matcher = ANCHOR_PATTERN.matcher(line);
				if (matcher.matches()) {
					String anchor = matcher.group(1);
					String previous = anchors.put(anchor, current);
					if (previous != null) {
						error("Anchor '%s' exists in both %s and %s.%n", anchor,
								previous, current);
					}
				}

				matcher = LINK_PATTERN.matcher(line);
				while (matcher.find()) {
					links.add(new Link(current, lineNumber, matcher.group(1), matcher.group(2)));
				}
			}
			reader.close();
		}

		for (Link link : links) {
			String target = anchors.get(link.targetAnchor);
			if (target == null) {
				error("The link at %s@%d pointing to %s#%s references an undefined anchor.%n",
						link.owningFile, link.line, link.targetFile, link.targetAnchor);
			}
			else if (!target.equals(link.targetFile)) {
				error(
						"The link at %s@%d pointing to %s#%s references an anchor that is actually defined in '%s'.%n",
						link.owningFile, link.line, link.targetFile, link.targetAnchor, target);
			}
		}


	}

	/**
	 * Report an error.
	 */
	private void error(String string, Object... inserts) {
		errors.append(String.format(string, inserts));
		nbErrors++;
	}

	private String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		if (dot >= 0) {
			return filename.substring(0, dot);
		}
		else {
			return filename;
		}
	}

	private static class Link {

		private String owningFile;

		private String targetFile;

		private String targetAnchor;

		private int line;

		public Link(String owningFile, int line, String targetFile, String targetAnchor) {
			this.owningFile = owningFile;
			this.targetFile = targetFile;
			this.targetAnchor = targetAnchor;
			this.line = line;
		}


	}

}
