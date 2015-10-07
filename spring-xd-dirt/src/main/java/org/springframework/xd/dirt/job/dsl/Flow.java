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

package org.springframework.xd.dirt.job.dsl;

import java.util.Collections;
import java.util.List;

/**
 * The AST node representing a flow. A flow is a series of jobs to execute sequentially. Those jobs
 * can themselves be individual jobs or splits. In DSL form a flow is expressed like this:
 * <pre><tt>aa || bb</tt></pre>.
 *
 * @author Andy Clement
 */
public class Flow extends JobSeries {

	private List<JobNode> series;

	public Flow(List<JobNode> jobDefOrRefsWithConditions) {
		super(jobDefOrRefsWithConditions.get(0).getStartPos(),
				jobDefOrRefsWithConditions.get(jobDefOrRefsWithConditions.size() - 1).getEndPos());
		this.series = Collections.unmodifiableList(jobDefOrRefsWithConditions);
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < series.size(); i++) {
			if (i > 0) {
				s.append(" ").append(TokenKind.DOUBLE_PIPE.getTokenString()).append(" ");
			}
			s.append(series.get(i).stringify(includePositionInfo));
		}
		return s.toString();
	}

	@Override
	public int getSeriesLength() {
		return series.size();
	}

	@Override
	public List<JobNode> getSeries() {
		return series;
	}

	@Override
	public JobNode getSeriesElement(int index) {
		return series.get(index);
	}

	@Override
	boolean isFlow() {
		return true;
	}

	@Override
	public String toString() {
		return "[Flow:" + stringify(true) + "]";
	}
}
