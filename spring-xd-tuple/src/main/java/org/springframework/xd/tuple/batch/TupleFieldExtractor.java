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

package org.springframework.xd.tuple.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.tuple.Tuple;

/**
 * Implementation of the {@link FieldExtractor} that extracts fields from a {@link Tuple}. There are two options for the
 * extraction:
 * 
 * <ul>
 * <li>Names - The fields will be extracted from the item in the order they are specified via the names in the provided
 * List.</li>
 * <li>Order - Since Tuple implementations are ordered, if a list of names is not provided, all fields in the Tuple will
 * be extracted in order.
 * </ul>
 * 
 * @author Michael Minella
 * 
 */
public class TupleFieldExtractor implements FieldExtractor<Tuple> {

	private List<String> names;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.transform.FieldExtractor#extract(T item)
	 */
	@Override
	public Object[] extract(Tuple item) {
		List<Object> extractedFields = new ArrayList<Object>(item.getFieldCount());

		if (CollectionUtils.isEmpty(names)) {
			for (int i = 0; i < item.getFieldCount(); i++) {
				extractedFields.add(item.getValue(i));
			}
		}
		else {
			for (String fieldName : names) {
				extractedFields.add(item.getValue(fieldName));
			}
		}
		return extractedFields.toArray();
	}

	/**
	 * An ordered list of field names to be extracted from each item.
	 * 
	 * @param names ordered list of field names
	 */
	public void setNames(List<String> names) {
		this.names = names;
	}
}
