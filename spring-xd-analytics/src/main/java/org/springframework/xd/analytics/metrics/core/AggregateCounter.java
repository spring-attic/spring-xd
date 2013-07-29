/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.core;

import java.util.HashMap;
import java.util.Map;

/**
 * A counter that tracks integral values but also remembers how its value was distributed
 * over time.
 * 
 * <p>
 * This core class only holds data structures. Depending on backing stores, logic for
 * computing totals may be implemented in a specialization of this class or at the
 * repository level.
 * </p>
 * 
 * @author Luke Taylor
 * @author Eric Bottard
 */
public class AggregateCounter implements Metric {

	protected String name;

	protected int total;

	protected Map<Integer, int[]> monthCountsByYear = new HashMap<Integer, int[]>();

	protected Map<Integer, int[]> dayCountsByYear = new HashMap<Integer, int[]>();

	protected Map<Integer, int[]> hourCountsByDay = new HashMap<Integer, int[]>();

	protected Map<Integer, int[]> minuteCountsByDay = new HashMap<Integer, int[]>();

	public AggregateCounter(String name) {
		this.name = name;
	}

	public AggregateCounter(AggregateCounter other) {
		this.name = other.name;
		this.total = other.total;
		this.monthCountsByYear = other.monthCountsByYear;
		this.dayCountsByYear = other.dayCountsByYear;
		this.hourCountsByDay = other.hourCountsByDay;
		this.minuteCountsByDay = other.minuteCountsByDay;
	}

	public int getTotal() {
		return total;
	}

	@Override
	public String getName() {
		return name;
	}
}