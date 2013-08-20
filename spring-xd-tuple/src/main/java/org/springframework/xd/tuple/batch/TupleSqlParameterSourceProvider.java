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

import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.xd.tuple.Tuple;

/**
 * A factory for {@link TupleSqlParameterSource}s
 * 
 * @author Michael Minella
 * 
 */
public class TupleSqlParameterSourceProvider implements ItemSqlParameterSourceProvider<Tuple> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.database.ItemSqlParameterSourceProvider#createSqlParameterSource(T item)
	 */
	@Override
	public SqlParameterSource createSqlParameterSource(Tuple item) {
		return new TupleSqlParameterSource(item);
	}
}
