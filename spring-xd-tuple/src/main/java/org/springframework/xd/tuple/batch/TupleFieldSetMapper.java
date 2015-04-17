/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.tuple.batch;

import java.text.DateFormat;
import java.util.Map;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.xd.tuple.DefaultTupleConversionService;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * A {@link FieldSetMapper} implementation intended to allow a user use a {@link Tuple} as the item. By default, all
 * fields of the {@link FieldSet} will be mapped as Strings. To use type specific mapping, provide a map of field names
 * to {@link FieldSetType}. You are not required to map all fields if using type specific mappings (only the ones that
 * matter).
 * 
 * @author Michael Minella
 * 
 */
public class TupleFieldSetMapper implements FieldSetMapper<Tuple> {

	// TODO: Is one date format good enough or will we need to be able to map formats to fields?
	private DateFormat dateFormat;

	// TODO: Currently this is bound by the convenience methods on the Tuple object. Is custom conversion necessary?
	private Map<String, FieldSetType> types;

	private FormattingConversionService defaultConversionService = new DefaultTupleConversionService();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.FieldSetMapper#mapFieldSet(FieldSet fieldSet)
	 */
	@Override
	public Tuple mapFieldSet(FieldSet fieldSet) throws BindException {
		String[] names = fieldSet.getNames();

		TupleBuilder builder = TupleBuilder.tuple();

		if (dateFormat != null) {
			builder.setFormats(null, dateFormat).setConfigurableConversionService(defaultConversionService);
		}

		for (int i = 0; i < fieldSet.getFieldCount(); i++) {
			builder.put(names[i], getValue(fieldSet, names[i]));
		}

		return builder.build();
	}

	private Object getValue(FieldSet fs, String name) {
		if (!CollectionUtils.isEmpty(types)) {
			FieldSetType type = types.get(name);

			if (type == null) {
				return fs.readString(name);
			}
			else if (type == FieldSetType.BIG_DECIMAL) {
				return fs.readBigDecimal(name);
			}
			else if (type == FieldSetType.BOOLEAN) {
				return fs.readBoolean(name);
			}
			else if (type == FieldSetType.BYTE) {
				return fs.readByte(name);
			}
			else if (type == FieldSetType.CHAR) {
				return fs.readChar(name);
			}
			else if (type == FieldSetType.DATE) {
				return fs.readDate(name);
			}
			else if (type == FieldSetType.DOUBLE) {
				return fs.readDouble(name);
			}
			else if (type == FieldSetType.FLOAT) {
				return fs.readFloat(name);
			}
			else if (type == FieldSetType.INT) {
				return fs.readInt(name);
			}
			else if (type == FieldSetType.LONG) {
				return fs.readLong(name);
			}
			else if (type == FieldSetType.SHORT) {
				return fs.readShort(name);
			}
			else if (type == FieldSetType.STRING) {
				return fs.readString(name);
			}
			else {
				throw new UnsupportedOperationException("Unable to determine the type to retrieve for " + name);
			}
		}
		else {
			return fs.readString(name);
		}
	}

	/**
	 * The {@link DateFormat} used by the resulting {@link Tuple} instance returned when converting strings to Dates.
	 * 
	 * @param formatter The format any dates provided will be in.
	 */
	public void setDateFormat(DateFormat formatter) {
		this.dateFormat = formatter;
	}

	/**
	 * A map of {@link FieldSet} field names to {@link FieldSetType}.
	 * 
	 * @param types mapping of field names to data types
	 */
	public void setTypes(Map<String, FieldSetType> types) {
		this.types = types;
	}
}
