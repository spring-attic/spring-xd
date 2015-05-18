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

package org.springframework.xd.dirt.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.xd.module.ModuleDescriptor;

/**
 * Represents the result of parsing a multiline document, made of several definitions.
 *
 * <p>This class is used to report per-line success or failure of the parser.</p>
 *
 * @author Eric Bottard
 */
public class DocumentParseResult implements Iterable<DocumentParseResult.Line> {

	private List<Line> lines;

	public DocumentParseResult(int size) {
		lines = new ArrayList<>(size);
	}

	public void success(List<ModuleDescriptor> descriptors, List<Exception> errors) {
		lines.add(new Line(descriptors, errors));
	}

	public void failure(Exception e) {
		lines.add(new Line(e));
	}

	@Override
	public Iterator<Line> iterator() {
		return lines.iterator();
	}

	/**
	 * The parse result of an individual line.
	 *
	 * @author Eric Bottard
	 */
	public static class Line {

		private final List<ModuleDescriptor> descriptors;

		private final List<Exception> exceptions;

		private Line(Exception e) {
			this.exceptions = new ArrayList<>();
			this.exceptions.add(e);
			this.descriptors = null;
		}

		private Line(List<ModuleDescriptor> moduleDescriptors, List<Exception> errors) {
			this.descriptors = moduleDescriptors;
			this.exceptions = errors.size() > 0 ? errors : null;
		}

		private Line(List<ModuleDescriptor> moduleDescriptors) {
			this.descriptors = moduleDescriptors;
			this.exceptions = null;
		}

		public List<Exception> getExceptions() {
			return exceptions;
		}

		public List<ModuleDescriptor> getDescriptors() {
			return descriptors;
		}
	}
}
