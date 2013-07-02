package org.springframework.xd.analytics.metrics.core;

import java.util.List;

/**
 * Utility class, primarily to avoid exposing mutable objects beyond the core package.
 *
 * For internal use only.
 */
public final class MetricUtils {

	public static Counter incrementCounter(Counter c) {
		return c.set(c.getValue() + 1);
	}

	public static Counter decrementCounter(Counter c) {
		return c.set(c.getValue() - 1);
	}

	public static Counter resetCounter(Counter c) {
		return c.set(0);
	}

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

	public static int[] concatArrays(List<int[]> arrays, int start, int size, int subSize) {
		int[] counts = new int[size];

		for (int i=0; i < arrays.size(); i++) {
			int[] sub = arrays.get(i);
			for (int j=0; j < subSize; j++) {
				int index = i*subSize + j - start; // index in the complete interval

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
