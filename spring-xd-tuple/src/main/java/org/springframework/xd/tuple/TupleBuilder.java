/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.tuple;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.Assert;

/**
 * Builder class to create Tuple instances.
 * 
 * Default Locale is US for NumberFormat and default DatePattern is "yyyy-MM-dd"
 * 
 * @author Mark Pollack
 * @author David Turanski
 * 
 */
public class TupleBuilder {

	private List<String> names = new ArrayList<String>();

	private List<Object> values = new ArrayList<Object>();

	private FormattingConversionService formattingConversionService = new DefaultTupleConversionService();

	private final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

	private final static Locale DEFAULT_LOCALE = Locale.US;

	private static Converter<Tuple, String> tupleToStringConverter = new TupleToJsonStringConverter();

	private static Converter<String, Tuple> stringToTupleConverter = new JsonStringToTupleConverter();

	private DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
	{
		dateFormat.setLenient(false);
	}

	public static TupleBuilder tuple() {
		TupleBuilder tb = new TupleBuilder();
		tb.setNumberFormatFromLocale(DEFAULT_LOCALE);
		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
		dateFormat.setLenient(false);
		tb.setDateFormat(dateFormat);
		return tb;
	}

	public Tuple of(String k1, Object v1) {
		return newTuple(namesOf(k1), valuesOf(v1));
	}

	public Tuple of(String k1, Object v1, String k2, Object v2) {
		addEntry(k1, v1);
		addEntry(k2, v2);
		return build();
	}

	public Tuple of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		addEntry(k1, v1);
		addEntry(k2, v2);
		addEntry(k3, v3);
		return build();
	}

	public Tuple of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		addEntry(k1, v1);
		addEntry(k2, v2);
		addEntry(k3, v3);
		addEntry(k4, v4);
		return build();
	}

	public Tuple ofNamesAndValues(List<String> names, List<Object> values) {
		this.names = names;
		this.values = values;
		return build();
	}

	public TupleBuilder put(String k1, Object v1) {
		addEntry(k1, v1);
		return this;
	}

	public Tuple build() {
		return newTuple(names, values);
	}

	public static Tuple fromString(String source) {
		return stringToTupleConverter.convert(source);
	}

	public TupleBuilder setFormattingConversionService(FormattingConversionService formattingConversionService) {
		Assert.notNull(formattingConversionService);
		this.formattingConversionService = formattingConversionService;
		return this;
	}

	public TupleBuilder setNumberFormatFromLocale(Locale locale) {
		Assert.notNull(locale);
		formattingConversionService.addConverterFactory(new LocaleAwareStringToNumberConverterFactory(NumberFormat
				.getInstance(locale)));
		return this;
	}

	public TupleBuilder setDateFormat(DateFormat dateFormat) {
		Assert.notNull(dateFormat);
		formattingConversionService.addConverter(new StringToDateConverter(dateFormat));
		return this;
	}

	void addEntry(String k1, Object v1) {
		names.add(k1);
		values.add(v1);
	}

	static List<Object> valuesOf(Object v1) {
		ArrayList<Object> values = new ArrayList<Object>();
		values.add(v1);
		return Collections.unmodifiableList(values);
	}

	static List<String> namesOf(String k1) {
		List<String> fields = new ArrayList<String>(1);
		fields.add(k1);
		return Collections.unmodifiableList(fields);

	}

	protected Tuple newTuple(List<String> names, List<Object> values) {
		DefaultTuple tuple = new DefaultTuple(names, values, formattingConversionService);
		tuple.setTupleToStringConverter(tupleToStringConverter);
		return tuple;
	}

}
