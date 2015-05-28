/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.greenplum.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base settings for all external tables;
 *
 * @since 1.2
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
public abstract class AbstractExternalTable {

	// LOCATION
	private List<String> locations;

	// FORMAT 'TEXT'|'CVS'
	private Format format;

	// [DELIMITER [AS] 'delimiter' | 'OFF']
	private Character delimiter;

	// [NULL [AS] 'null string']
	private String nullString;

	// [ESCAPE [AS] 'escape' | 'OFF']
	private Character escape;

	// [QUOTE [AS] 'quote']
	private Character formatQuote;

	// [FORCE NOT NULL column [, ...]]
	private String[] formatForceQuote;

	// [ ENCODING 'encoding' ]
	private String encoding;

	private String like;

	private String columns;

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = new ArrayList<String>(locations);
	}

	public void setTextFormat() {
		this.format = Format.TEXT;
	}

	public void setTextFormat(Character delimiter, String nullString, Character escape) {
		this.format = Format.TEXT;
		this.delimiter = delimiter;
		this.nullString = nullString;
		this.escape = escape;
	}

	public void setCsvFormat() {
		this.format = Format.CSV;
	}

	public void setCsvFormat(Character quote, Character delimiter, String nullString, String[] forceQuote,
			Character escape) {
		this.format = Format.CSV;
		this.formatQuote = quote;
		this.delimiter = delimiter;
		this.nullString = nullString;
		this.escape = escape;
		this.formatForceQuote = Arrays.copyOf(forceQuote, forceQuote.length);
	}

	public Format getFormat() {
		return format;
	}

	public Character getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(Character delimiter) {
		this.delimiter = delimiter;
	}

	public String getNullString() {
		return nullString;
	}

	public void setNullString(String nullString) {
		this.nullString = nullString;
	}

	public Character getEscape() {
		return escape;
	}

	public void setEscape(Character escape) {
		this.escape = escape;
	}

	public Character getQuote() {
		return formatQuote;
	}

	public void setQuote(Character quote) {
		this.formatQuote = quote;
	}

	public String[] getForceQuote() {
		return formatForceQuote;
	}

	public void setForceQuote(String[] forceQuote) {
		this.formatForceQuote = Arrays.copyOf(forceQuote, forceQuote.length);
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getLike() {
		return like;
	}

	public void setLike(String like) {
		this.like = like;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

}
