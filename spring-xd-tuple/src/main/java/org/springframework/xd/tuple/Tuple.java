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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Data structure that stores key-value pairs and adds several methods to access values in a type-safe manner
 * 
 * @author Mark Pollack
 *
 */
public interface Tuple {

	/**
	 * Return the number of elements in this tuple.
	 * @return number of elements
	 */
	int size();
	
	/**
	 * Return the fields names that can reference elements by name in this tuple
	 * @return list of field names
	 */
	List<String> getFieldNames();
	
	/**
	 * Return the values for all the fields in this tuple
	 * @return list of values.
	 */
	List<Object> getValues();
	
	/**
	 * Return true if the tuple contains a field of the given name
	 * @param name the name of the field
	 * @return true if present, otherwise false
	 */
	boolean hasFieldName(String name);
	
	/**
	 * Return the Java types of the fields in this tuple.
	 * @return the Java types of the fields in this tuple.
	 */
	@SuppressWarnings("rawtypes")
	List<Class> getFieldTypes();
	
	/**
	 * Return the number of fields in this tuple.
	 * @return the number of fields in this tuple.
	 */
	int getFieldCount();
	
	/**
	 * Return the value of the field given the name
	 * @param name the name of the field
	 * @return value of the field
	 * @throws IllegalArgumentException if the name is not present
	 */
	Object getValue(String name);
	
	/**
	 * Return the value of the field given the index position
	 * @param index position in the tuple
	 * @return value of the field
	 * @throws IndexOutOfBoundsException if the index position is out of bounds.
	 */
	Object getValue(int index);
	
	<T> T getValue(String name, Class<T> valueClass);
	
	<T> T getValue(int index, Class<T> valueClass);
	
	// Various getters to match up with Spring Batch needs.
	
	String getString(String name);
	
	String getString(int index);
	
	Character getChar(int index);
	
	Character getChar(String name);

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Boolean getBoolean(int index);
	
	/**
	 * Read the '<code>boolean</code>' value from field with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a field with given name is not
	 * defined.
	 */
	Boolean getBoolean(String name);

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @param trueValue the value that signifies {@link Boolean#TRUE true};
	 * case-sensitive.
	 * @throws IndexOutOfBoundsException if the index is out of bounds, or if
	 * the supplied <code>trueValue</code> is <code>null</code>.
	 */
	Boolean getBoolean(int index, String trueValue);
	
	/**
	 * Read the '<code>boolean</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @param trueValue the value that signifies {@link Boolean#TRUE true};
	 * case-sensitive.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined, or if the supplied <code>trueValue</code> is <code>null</code>.
	 */
	Boolean getBoolean(String name, String trueValue);
	
	/**
	 * Read the '<code>byte</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Byte getByte(int index);
	
	/**
	 * Read the '<code>byte</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Byte getByte(String name);
	

	/**
	 * Read the '<code>short</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Short getShort(int index);
	
	/**
	 * Read the '<code>short</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Short getShort(String name);
	
	/**
	 * Read the '<code>int</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Integer getInt(int index);
	
	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Integer getInt(String name); 
	
	/**
	 * Read the '<code>long</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Long getLong(int index);
	
	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Long getLong(String name); 
	
	/**
	 * Read the '<code>float</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Float getFloat(int index);
	
	/**
	 * Read the '<code>float</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Float getFloat(String name);	
	
	/**
	 * Read the '<code>double</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	Double getDouble(int index);
	
	/**
	 * Read the '<code>double</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	Double getDouble(String name);	
	
	/**
	 * Read the '<code>BigDecimal</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	BigDecimal getBigDecimal(int index);
	
	/**
	 * Read the '<code>BigDecimal</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	BigDecimal getBigDecimal(String name);
	
	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws ConversionFailedException if the value is not parseable
	 */
	Date getDate(int index);
	
	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not defined
	 * @throws ConversionFailedException if the value is not parseable
	 */
	Date getDate(String name);
	
	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @param pattern the pattern describing the date and time format
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws IllegalArgumentException if the date cannot be parsed.
	 * 
	 */
	Date getDate(int index, String pattern);
	
	/**
	 * Read the <code>java.util.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @param pattern the pattern describing the date and time format
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined or if the specified field cannot be parsed
	 * 
	 */
	Date getDate(String name, String pattern);
	
	/**
	 * Use SpEL expression to return a subset of the tuple that matches the expression
	 * @param expression SpEL expression to select from a Map, e.g. ?[key.startsWith('b')]
	 * @return a new Tuple with data selected from the current instance.
	 */
	Tuple select(String expression);
	
	// Meta-data useful for stream processing
	
	/**
	 * Get the unique Id of this tuple, not included in comparisons for equality.
	 * @return unique Id
	 */
	UUID getId();
	
	/**
	 * Get the creation timestamp of this tuple, not included in comparisons for equality
	 * @return creation timestamp
	 */
	Long getTimestamp();


	/* TODO consider once tuples are being processed in some context
	String getStreamName();
	
	String getComponentName();
	*/
	
	
	/* TODO consider general metadata property/hash */
	
	/* TODO in FieldSet have getRawString (including trailing white space) as well as getProperties */

}
