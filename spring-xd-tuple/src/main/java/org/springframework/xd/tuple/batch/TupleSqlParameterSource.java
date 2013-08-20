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

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;
import org.springframework.xd.tuple.Tuple;

/**
 * Implementation of a {@link SqlParameterSource} that translates a {@link Tuple} into SQL parameters.
 * 
 * @author Michael Minella
 * 
 */
public class TupleSqlParameterSource extends AbstractSqlParameterSource {

	private Tuple tuple;

	public TupleSqlParameterSource(Tuple item) {
		Assert.notNull(item, "An item is required");
		this.tuple = item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.jdbc.core.namedparam.SqlParameterSource#hasValue(String paramName)
	 */
	@Override
	public boolean hasValue(String paramName) {
		return tuple.hasFieldName(paramName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.jdbc.core.namedparam.SqlParameterSource#getValue(String paramName)
	 */
	@Override
	public Object getValue(String paramName) throws IllegalArgumentException {
		return tuple.getValue(paramName);
	}
}
