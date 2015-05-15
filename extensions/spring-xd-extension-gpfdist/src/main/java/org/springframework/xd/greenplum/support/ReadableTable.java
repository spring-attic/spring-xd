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

/**
 * Settings for readable external table.
 *
 * @author Janne Valkealahti
 *
 */
public class ReadableTable extends AbstractExternalTable {

	// [LOG ERRORS INTO error_table]
	private String logErrorsInto;

	// SEGMENT REJECT LIMIT count
	private Integer segmentRejectLimit;

	// [ROWS | PERCENT]
	private SegmentRejectType segmentRejectType;

	// FORMAT 'TEXT|CVS' [( [HEADER]
	private boolean formatHeader;

	public boolean isFormatHeader() {
		return formatHeader;
	}

	public void setFormatHeader(boolean formatHeader) {
		this.formatHeader = formatHeader;
	}

	public String getLogErrorsInto() {
		return logErrorsInto;
	}

	public void setLogErrorsInto(String logErrorsInto) {
		this.logErrorsInto = logErrorsInto;
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

}
