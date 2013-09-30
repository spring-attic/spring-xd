
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

	public static RichGauge setRichGaugeValue(RichGauge g, double value) {
		return g.set(value);
	}

	public static RichGauge setRichGaugeAlpha(RichGauge g, double value) {
		return g.setAlpha(value);
	}

	public static RichGauge resetRichGauge(RichGauge g) {
		return g.reset();
	}

	public static long[] concatArrays(List<long[]> arrays, int start, int size) {
		long[] counts = new long[size];
		long[] first = arrays.remove(0);
		int index = Math.min(first.length - start, size);
		System.arraycopy(first, start, counts, 0, index);
		for (int i = 0; i < arrays.size(); i++) {
			long[] sub = arrays.get(i);
			for (int j = 0; j < sub.length; j++) {
				if (index >= counts.length) {
					break;
				}

				counts[index++] = sub[j];
			}
		}

		return counts;
	}

}
