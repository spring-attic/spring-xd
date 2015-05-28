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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @since 1.2
 * @author Janne Valkealahti
 * @author Gary Russell
 */
public class ReadableTableFactoryBean implements FactoryBean<ReadableTable>, InitializingBean {

	private ControlFile controlFile;

	private List<String> locations;

	private String columns;

	private String like;

	private boolean keeptable;

	private Format format = Format.TEXT;

	private Character delimiter;

	private String nullString;

	private Character escape;

	private Character quote;

	private String[] forceQuote;

	private String logErrorsInto;

	private Integer segmentRejectLimit;

	private SegmentRejectType segmentRejectType;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (controlFile != null) {
			if (controlFile.getGploadInputDelimiter() != null) {
				this.delimiter = controlFile.getGploadInputDelimiter();
			}
		}
	}

	@Override
	public ReadableTable getObject() throws Exception {
		ReadableTable w = new ReadableTable();
		w.setLocations(locations);
		w.setColumns(columns);
		w.setLike(like);
		w.setLogErrorsInto(logErrorsInto);
		w.setSegmentRejectLimit(segmentRejectLimit);
		w.setSegmentRejectType(segmentRejectType);

		if (format == Format.TEXT) {
			Character delim = delimiter != null ? delimiter : Character.valueOf('\t');
			w.setTextFormat(delim, nullString, escape);
		}
		else if (format == Format.CSV) {
			Character delim = delimiter != null ? delimiter : Character.valueOf(',');
			w.setCsvFormat(quote, delim, nullString, forceQuote, escape);
		}

		return w;
	}

	@Override
	public Class<ReadableTable> getObjectType() {
		return ReadableTable.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setControlFile(ControlFile controlFile) {
		this.controlFile = controlFile;
	}

	public Integer getSegmentRejectLimit() {
		return segmentRejectLimit;
	}

	public void setSegmentRejectLimit(Integer segmentRejectLimit) {
		this.segmentRejectLimit = segmentRejectLimit;
	}

	public SegmentRejectType getSegmentRejectType() {
		return segmentRejectType;
	}

	public void setSegmentRejectType(SegmentRejectType segmentRejectType) {
		this.segmentRejectType = segmentRejectType;
	}

	public String getLogErrorsInto() {
		return logErrorsInto;
	}

	public void setLogErrorsInto(String logErrorsInto) {
		this.logErrorsInto = logErrorsInto;
	}

	public Character getQuote() {
		return quote;
	}

	public void setQuote(Character quote) {
		this.quote = quote;
	}

	public String[] getForceQuote() {
		return forceQuote;
	}

	public void setForceQuote(String[] forceQuote) {
		this.forceQuote = Arrays.copyOf(forceQuote, forceQuote.length);
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

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getLike() {
		return like;
	}

	public void setLike(String like) {
		this.like = like;
	}

	public boolean isKeeptable() {
		return keeptable;
	}

	public void setKeeptable(boolean keeptable) {
		this.keeptable = keeptable;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

}
