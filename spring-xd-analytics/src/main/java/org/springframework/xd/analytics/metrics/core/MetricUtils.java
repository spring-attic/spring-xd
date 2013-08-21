
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

	public static long[] concatArrays(List<long[]> arrays, int start, int size, int subSize) {
		long[] counts = new long[size];

		for (int i = 0; i < arrays.size(); i++) {
			long[] sub = arrays.get(i);
			for (int j = 0; j < subSize; j++) {
				int index = i * subSize + j - start; // index in the complete interval

				if (index >= counts.length) {
					break;
				}

				if (index >= 0) {
					counts[index] = sub[j];
				}
			}
		}

		return counts;
	}

}
