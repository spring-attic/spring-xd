package org.springframework.xd.analytics.metrics.core;

/**
 * Utility class to avoid exposing mutable Counters beyond the core package.
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

	public static RichGauge resetRichGauge(RichGauge g) {
		return g.reset();
	}
}
