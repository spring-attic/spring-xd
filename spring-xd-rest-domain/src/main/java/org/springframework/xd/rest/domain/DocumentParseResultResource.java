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

package org.springframework.xd.rest.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.ResourceSupport;

/**
 * Represents the result of parsing a multiline definition document.
 *
 * @author Eric Bottard
 */
public class DocumentParseResultResource extends ResourceSupport {

	private List<Line> lines = new ArrayList<>();

	public void addLine(Line line) {
		lines.add(line);
	}

	public List<Line> getLines() {
		return lines;
	}

	/**
	 * The result of parsing a single line of the input document.
	 *
	 * <p>At most one of {@code errors} or {@code success} is non-null.</p>
	 *
	 * @author Eric Bottard
	 */
	public static class Line {

		private List<Error> errors;

		private List<ModuleDescriptor> success;

		public void addError(Error error) {
			if (errors == null) {
				errors = new ArrayList<>();
			}
			errors.add(error);
		}

		public void addDescriptor(ModuleDescriptor descriptor) {
			if (success == null) {
				success = new ArrayList<>();
			}
			success.add(descriptor);
		}

		public List<Error> getErrors() {
			return errors;
		}

		public List<ModuleDescriptor> getSuccess() {
			return success;
		}
	}

	public static class Error {

		/**
		 * The message of the error, as reported by the caught exception.
		 */
		private String message;

		/**
		 * If available, the position inside the line definition at which the error has been detected.
		 */
		private Integer position;

		public Error(String message) {
			this(message, null);
		}

		public Error(String message, Integer position) {
			this.message = message;
			this.position = position;
		}

		public String getMessage() {
			return message;
		}

		public Integer getPosition() {
			return position;
		}
	}

	public static class ModuleDescriptor {

		/**
		 * The group to which this module belongs.
		 */
		private String group;

		/**
		 * The effective label which uniquely identifies this module in the group. May be different from the name.
		 */
		private String label;

		/**
		 * The type of the module.
		 */
		private RESTModuleType type;

		/**
		 * The name of the module which, together with the type, tells us which module to use.
		 */
		private String name;

		/**
		 * The options that were explicitly used for this module.
		 */
		private Map<String, String> options;

		/**
		 * Name of source channel, if defined by the stream/job definition.
		 * May be {@code null}.
		 */
		private String sourceChannelName;

		/**
		 * Name of sink channel, if defined by the stream/job definition.
		 * May be {@code null}.
		 */
		private String sinkChannelName;

		public ModuleDescriptor(String group, String moduleLabel, RESTModuleType type, String name,
				String sourceChannelName, String sinkChannelName, Map<String, String> options) {
			this.group = group;
			this.label = moduleLabel;
			this.type = type;
			this.name = name;
			this.sourceChannelName = sourceChannelName;
			this.sinkChannelName = sinkChannelName;
			this.options = options;
		}

		public String getGroup() {
			return group;
		}

		public String getLabel() {
			return label;
		}

		public RESTModuleType getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public Map<String, String> getOptions() {
			return options;
		}

		public String getSourceChannelName() {
			return sourceChannelName;
		}

		public String getSinkChannelName() {
			return sinkChannelName;
		}
	}
}
