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

import java.util.List;

import org.springframework.util.StringUtils;

public abstract class SqlUtils {

	public static String createExternalReadableTable(LoadConfiguration config, String prefix,
			List<String> overrideLocations) {

		// TODO: this function needs a cleanup
		StringBuilder buf = new StringBuilder();

		// unique table name
		String name = config.getTable() + "_ext_" + prefix;
		buf.append("CREATE READABLE EXTERNAL TABLE ");
		buf.append(name);
		buf.append(" ( ");

		// column types or like
		ReadableTable externalTable = config.getExternalTable();
		if (externalTable.getLike() != null) {
			buf.append("LIKE ");
			buf.append(config.getTable());
		}
		else if (StringUtils.hasText(externalTable.getColumns())) {
			buf.append(externalTable.getColumns());
		}
		else {
			buf.append("LIKE ");
			buf.append(config.getTable());
		}
		buf.append(" ) ");

		// locations
		buf.append("LOCATION(");
		if (overrideLocations != null && !overrideLocations.isEmpty()) {
			buf.append(createLocationString(overrideLocations.toArray(new String[0])));
		}
		else {
			buf.append(createLocationString(externalTable.getLocations().toArray(new String[0])));
		}
		buf.append(") ");

		// format type
		if (externalTable.getFormat() == Format.TEXT) {
			buf.append("FORMAT 'TEXT'");
		}
		else {
			buf.append("FORMAT 'CSV'");
		}

		// format parameters
		buf.append(" ( ");
		buf.append("DELIMITER '");
		if (externalTable.getDelimiter() != null) {
			buf.append(unicodeEscaped(externalTable.getDelimiter().charValue()));
		}
		else {
			buf.append("|");
		}
		buf.append("'");

		if (externalTable.getNullString() != null) {
			buf.append(" NULL '");
			buf.append(externalTable.getNullString());
			buf.append("'");
		}

		if (externalTable.getEscape() != null) {
			buf.append(" ESCAPE '");
			buf.append(externalTable.getEscape());
			buf.append("'");
		}

		if (externalTable.getQuote() != null) {
			buf.append(" QUOTE '");
			buf.append(externalTable.getQuote());
			buf.append("'");
		}

		if (externalTable.getForceQuote() != null) {
			buf.append(" FORCE QUOTE ");
			buf.append(StringUtils.arrayToCommaDelimitedString(externalTable.getForceQuote()));
		}

		buf.append(" )");

		if (externalTable.getEncoding() != null) {
			buf.append(" ENCODING '");
			buf.append(externalTable.getEncoding());
			buf.append("'");
		}

		if (externalTable.getSegmentRejectLimit() != null && externalTable.getSegmentRejectType() != null) {
			if (externalTable.getLogErrorsInto() != null) {
				buf.append(" LOG ERRORS INTO ");
				buf.append(externalTable.getLogErrorsInto());
			}
			buf.append(" SEGMENT REJECT LIMIT ");
			buf.append(externalTable.getSegmentRejectLimit());
			buf.append(" ");
			buf.append(externalTable.getSegmentRejectType());
		}

		return buf.toString();
	}

	/**
	 *
	 * @param config the load configuration
	 * @param prefix the prefix
	 * @return the drop DDL
	 */
	public static String dropExternalReadableTable(LoadConfiguration config, String prefix) {
		StringBuilder b = new StringBuilder();

		// unique table name
		String name = config.getTable() + "_ext_" + prefix;

		b.append("DROP EXTERNAL TABLE ");
		b.append(name);

		return b.toString();

	}

	/**
	 * Builds sql clause to load data into a database.
	 *
	 * @param config
	 *            Load configuration.
	 * @param prefix
	 *            Prefix for temporary resources.
	 * @return
	 *            the load DDL
	 */
	public static String load(LoadConfiguration config, String prefix) {
		if (config.getMode() == Mode.INSERT) {
			return loadInsert(config, prefix);
		}
		else if (config.getMode() == Mode.UPDATE) {
			return loadUpdate(config, prefix);
		}
		throw new IllegalArgumentException("Unsupported mode " + config.getMode());
	}

	private static String loadInsert(LoadConfiguration config, String prefix) {
		StringBuilder b = new StringBuilder();

		String name = config.getTable() + "_ext_" + prefix;

		b.append("INSERT INTO ");
		b.append(config.getTable());
		b.append(" SELECT ");
		if (StringUtils.hasText(config.getColumns())) {
			b.append(config.getColumns());
		}
		else {
			b.append("*");
		}
		b.append(" FROM ");
		b.append(name);

		return b.toString();
	}

	private static String loadUpdate(LoadConfiguration config, String prefix) {
		StringBuilder b = new StringBuilder();
		String name = config.getTable() + "_ext_" + prefix;
		b.append("UPDATE ");
		b.append(config.getTable());
		b.append(" into_table set ");

		for (int i = 0; i < config.getUpdateColumns().size(); i++) {
			b.append(config.getUpdateColumns().get(i) + "=from_table." + config.getUpdateColumns().get(i));
			if (i + 1 < config.getUpdateColumns().size()) {
				b.append(", ");
			}
		}

		b.append(" FROM ");
		b.append(name);
		b.append(" from_table where ");

		for (int i = 0; i < config.getMatchColumns().size(); i++) {
			b.append("into_table." + config.getMatchColumns().get(i) + "=from_table." + config.getMatchColumns().get(i));
			if (i + 1 < config.getMatchColumns().size()) {
				b.append(" and ");
			}
		}

		if (StringUtils.hasText(config.getUpdateCondition())) {
			b.append(" and " + config.getUpdateCondition());
		}

		return b.toString();
	}

	/**
	 * Converts string array to greenplum friendly string. From new
	 * String[]{"foo","jee"} we get "'foo',jee'".
	 *
	 * @param strings
	 *            String array to explode
	 * @return Comma delimited string with values encapsulated with
	 *         apostropheres. ‘'
	 */
	public static String createLocationString(String[] strings) {
		StringBuilder locString = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i];
			locString.append("'");
			locString.append(string);
			locString.append("'");
			if (i < strings.length - 1) {
				locString.append(",");
			}
		}
		return locString.toString();
	}

	private static String unicodeEscaped(char ch) {
		if (ch < 0x10) {
			return "\\u000" + Integer.toHexString(ch);
		}
		else if (ch < 0x100) {
			return "\\u00" + Integer.toHexString(ch);
		}
		else if (ch < 0x1000) {
			return "\\u0" + Integer.toHexString(ch);
		}
		return "\\u" + Integer.toHexString(ch);
	}

}
