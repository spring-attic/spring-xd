/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.util;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;


/**
 * Utility class for paging support in repository accessor methods.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class PagingUtility<T extends Comparable<? super T>> {

	/**
	 * Get the paged data of the given list.
	 *
	 * @param pageable the paging information, must not be null
	 * @param list the list of content to paginate, must not be null
	 * @return the paginated implementation of the given list of <T>
	 * @throws PageNotFoundException in case an invalid page is requested
	 */
	public Page<T> getPagedData(Pageable pageable, List<T> list) throws PageNotFoundException {

		Assert.notNull(pageable, "Pagination info can't be null.");
		Assert.notNull(list, "The provided list must not be null.");

		final int offset = pageable.getOffset();
		final int to = Math.min(list.size(), offset + pageable.getPageSize());

		if (offset > to) {
			throw new PageNotFoundException(String.format("Page %s does not exist.", pageable.getPageNumber()));
		}

		if (CollectionUtils.isEmpty(list)) {
			return new PageImpl<T>(list);
		}

		Collections.sort(list);

		final List<T> data = list.subList(offset, to);
		return new PageImpl<T>(data, pageable, list.size());
	}

}
