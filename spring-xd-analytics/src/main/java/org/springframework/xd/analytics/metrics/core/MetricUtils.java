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

package org.springframework.xd.analytics.metrics.core;

import java.util.List;

/**
 * Utility class, primarily to avoid exposing mutable objects beyond the core package.
 *
 * For internal use only.
 */
public final class MetricUtils {

	public static Gauge setGaugeValue(Gauge g, long value) {
		return g.set(value);
	}

	public static RichGauge setRichGaugeValue(RichGauge g, double value, double alpha) {
		return g.set(value, alpha);
	}

	public static RichGauge resetRichGauge(RichGauge g) {
		return g.reset();
	}

	public static long[] concatArrays(List<long[]> arrays, int start, int size) {
		long[] result = new long[size];
		int howManyLeft = size;
		int targetPosition = 0;

		int from = start;
		for (int i = 0; i < arrays.size() && howManyLeft > 0; i++) {
			long[] current = arrays.get(i);
			int howMany = Math.min(current.length - from, howManyLeft);
			System.arraycopy(current, from, result, targetPosition, howMany);
			from = 0;
			howManyLeft -= howMany;
			targetPosition += howMany;
		}
		if (howManyLeft > 0) {
			throw new ArrayIndexOutOfBoundsException(
					String.format("Not enough data, short of %d elements", howManyLeft));
		}
		return result;
	}

	public static long sum(long[] array) {
		if (array == null) {
			return 0L;
		}
		long sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

}
