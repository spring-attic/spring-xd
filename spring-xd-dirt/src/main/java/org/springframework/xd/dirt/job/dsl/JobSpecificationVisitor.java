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

package org.springframework.xd.dirt.job.dsl;

/**
 * very basic visitor pattern for a JobSpecVisitor. Provide a concrete implementation to participate in the
 * visit.
 *
 * @param <T> the type of the context objects flowing across the visit
 */
public abstract class JobSpecificationVisitor<T> {

	/**
	 * Called to start a visit of a specific JobSpec.
	 */
	protected void accept(JobSpecification jobSpec) {
		T context = preJobSpecWalk();
		JobNode jn = jobSpec.getJobNode();
		if (jn != null) {
			context = walk(context, jn);
		}
		postJobSpecWalk(context, jobSpec);
	}

	public final T walk(T context, JobNode jn) {
		if (jn instanceof Flow) {
			return walk(context, (Flow) jn);
		}
		else if (jn instanceof JobDefinition) {
			return walk(context, (JobDefinition) jn);
		}
		else if (jn instanceof JobReference) {
			return walk(context, (JobReference) jn);
		}
		else if (jn instanceof Split) {
			return walk(context, (Split) jn);
		}
		else {
			throw new IllegalStateException("nyi:" + jn.getClass().getName());
		}
	}

	/**
	 * Override to provide code that will run just before the visit starts.
	 * @return optionally return some context that will be passed to the first real visit operation
	 */
	public T preJobSpecWalk() {
		return null;
	}

	/**
	 * Override to provide code that will run just after the visit completes.
	 * @param context the final context computed by the last of the main visit operations
	 * @param jobSpec the job specification being visited
	 */
	public void postJobSpecWalk(T context, JobSpecification jobSpec) {

	}

	/**
	 * Visit a sequence of jobs (flow).
	 * @param context any context this visitor is passing during visits
	 * @param jobSeries a JobNode representing a series of jobs run in sequence
	 * @return any context to pass along to the next visit calls
	 */
	public abstract T walk(T context, Flow jobSeries);

	public abstract T walk(T context, JobDefinition jobDefinition);

	public abstract T walk(T context, JobReference jobReference);

	public abstract T walk(T context, Split jobSeries);
}
