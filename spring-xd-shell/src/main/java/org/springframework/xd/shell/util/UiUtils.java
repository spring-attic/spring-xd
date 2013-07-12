/*
 * Copyright 2009-2013 the original author or authors.
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
 */
package org.springframework.xd.shell.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains utility methods for rendering data to a formatted console output.
 * E.g. it provides helper methods for rendering ASCII-based data tables.
 *
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @since 1.0
 *
 */
public final class UiUtils {
	public static final String HORIZONTAL_LINE = "-------------------------------------------------------------------------------\n";

	public static final int COLUMN_1 = 1;
	public static final int COLUMN_2 = 2;
	public static final int COLUMN_3 = 3;
	public static final int COLUMN_4 = 4;
	public static final int COLUMN_5 = 5;
	public static final int COLUMN_6 = 6;

	/**
	 * Prevent instantiation.
	 *
	 */
	private UiUtils() {
		throw new AssertionError();
	}

	/**
	 * Renders a textual representation of the list of provided Map data
	 *
	 * @param columns List of {@CloudApplication}
	 * @return The rendered table representation as String
	 *
	 */
	public static String renderMapDataAsTable(List<Map<String, Object>> data, List<String> columns) {

		Table table = new Table();

		int col = 0;
		for (String colName : columns) {
			col++;
			table.getHeaders().put(col, new TableHeader(colName));
			if (col >= 6) {
				break;
			}
		}

		for (Map<String, Object> dataRow : data) {

			TableRow tableRow = new TableRow();

			for (int i = 0; i < col; i++) {
				String value = dataRow.get(columns.get(i)).toString();
				table.getHeaders().get(i + 1).updateWidth(value.length());
				tableRow.addValue(i + 1, value);
			}

			table.getRows().add(tableRow);
		}

		return renderTextTable(table);
	}

	/**
	 * Renders a textual representation of provided parameter map.
	 *
	 * @param parameters Map of parameters (key, value)
	 * @return The rendered table representation as String
	 *
	 */
	public static String renderParameterInfoDataAsTable(Map<String, String> parameters) {
		final Table table = new Table();

		table.getHeaders().put(COLUMN_1, new TableHeader("Parameter"));
		table.getHeaders().put(COLUMN_2, new TableHeader("Value (Configured or Default)"));

		for (Entry<String, String> entry : parameters.entrySet()) {

			final TableRow tableRow = new TableRow();

			table.getHeaders().get(COLUMN_1).updateWidth(entry.getKey().length());
			tableRow.addValue(COLUMN_1, entry.getKey());

			table.getHeaders().get(COLUMN_2).updateWidth(entry.getValue() != null ? entry.getValue().length() : 0);
			tableRow.addValue(COLUMN_2, entry.getValue());

			table.getRows().add(tableRow);
		}

		return renderTextTable(table);
	}

	/**
	 * Renders a textual representation of the provided {@link Table}
	 *
	 * @param table Table data {@link Table}
	 * @return The rendered table representation as String
	 */
	public static String renderTextTable(Table table) {
		final String padding = "  ";
		final String headerBorder = getHeaderBorder(table.getHeaders());
		final StringBuilder textTable = new StringBuilder();

		for (TableHeader header : table.getHeaders().values()) {
			textTable.append(padding + CommonUtils.padRight(header.getName(), header.getWidth()));
		}

		textTable.append("\n");

		textTable.append(headerBorder);

		for (TableRow row : table.getRows()) {
			for (Entry<Integer, TableHeader> entry : table.getHeaders().entrySet()) {
				textTable.append(padding + CommonUtils.padRight(row.getValue(entry.getKey()), entry.getValue().getWidth()));
			}
			textTable.append("\n");
		}

		return textTable.toString();
	}

	/**
	 * Renders the Table header border, based on the map of provided headers.
	 *
	 * @param headers Map of headers containing meta information e.g. name+width of header
	 * @return Returns the rendered header border as String
	 */
	public static String getHeaderBorder(Map<Integer, TableHeader> headers) {

		final StringBuilder headerBorder = new StringBuilder();

		for (TableHeader header : headers.values()) {
			headerBorder.append(CommonUtils.padRight("  ", header.getWidth() + 2, '-'));
		}
		headerBorder.append("\n");

		return headerBorder.toString();
	}
}