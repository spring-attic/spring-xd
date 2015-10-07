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
 * The AST node representing a split. A split is a series of jobs to execute in parallel. Those jobs
 * can themselves be individual jobs or splits. In DSL form a flow is expressed like this:
 * <pre><tt><aa & bb></tt></pre>.
 *
 * @author Andy Clement
 */
public class Split extends JobSeries {

	private List<JobNode> jobsInParallel;

	public Split(List<JobNode> parallelSequences) {
		super(parallelSequences.get(0).getStartPos(),
				parallelSequences.get(parallelSequences.size() - 1).getEndPos());
		this.jobsInParallel = Collections.unmodifiableList(parallelSequences);
	}


	@Override
	public String stringify(boolean includePositionInfo) {
		if (jobsInParallel.size() == 1) {
			return jobsInParallel.get(0).stringify(includePositionInfo);
		}
		else {
			StringBuilder s = new StringBuilder(TokenKind.SPLIT_OPEN.getTokenString());
			for (int i = 0; i < jobsInParallel.size(); i++) {
				JobNode jn = jobsInParallel.get(i);
				if (i > 0) {
					s.append(" ").append(TokenKind.AMPERSAND.getTokenString()).append(" ");
				}
				s.append(jn.stringify(includePositionInfo));
			}
			s.append(TokenKind.SPLIT_CLOSE.getTokenString());
			return s.toString();
		}
	}

	@Override
	public int getSeriesLength() {
		return jobsInParallel.size();
	}

	@Override
	public JobNode getSeriesElement(int index) {
		return jobsInParallel.get(index);
	}

	@Override
	public List<JobNode> getSeries() {
		return jobsInParallel;
	}

	@Override
	boolean isSplit() {
		return true;
	}

	@Override
	public String toString() {
		return "[Split:" + stringify(true) + "]";
	}

}
