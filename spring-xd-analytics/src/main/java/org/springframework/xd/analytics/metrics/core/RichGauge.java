package org.springframework.xd.analytics.metrics.core;

import org.springframework.util.Assert;

/**
 * A gauge which stores the maximum, minimum and simple mean in addition to the current value.
 *
 * @author Luke Taylor
 */
public final class RichGauge implements Metric {
	private final String name;
	private double value;
	private double mean;
	private double max;
	private double min;
	private long count;

	/**
	 * Creates an "empty" gauge.
	 *
	 * The mean, max and min will be zero, but this initial value will not be
	 * included after the first value has been set on the gauge.
	 *
	 * @param name the name under which the gauge will be stored.
	 */
	public RichGauge(String name) {
		this(name, 0.0);
		count = 0;
	}

	public RichGauge(String name, double value) {
		Assert.notNull(name, "The gauge name cannot be null or empty");
		this.name = name;
		max = min = mean = this.value = value;
		count = 1;
	}

	public RichGauge(String name, double value, double mean, double max, double min, long count) {
		this.name = name;
		this.value = value;
		this.mean = mean;
		this.max = max;
		this.min = min;
		this.count = count;
	}

	/**
	 * @return the name of the gauge
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the current value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @return The mean over all the accumulated values
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * @return the maximum value
	 */
	public double getMax() {
		return max;
	}

	/**
	 * @return the minimum value
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return Number of times the value has been set.
	 */
	public long getCount() {
		return count;
	}

	RichGauge set(double value) {
		if (count == 0) {
			max = min = value;
		} else if (value > max) {
			max = value;
		} else if (value < min) {
			min = value;
		}
		double sum = mean * count;
		this.value = value;
		sum += value;
		count++;
		mean = sum / count;

		return this;
	}

	RichGauge reset() {
		this.value = max = min = mean = 0.0;
		count = 0;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		RichGauge richGauge = (RichGauge) o;

		if (!name.equals(richGauge.name))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "Gauge [name=" + name + ", value=" + value + ", mean = " + mean +
				", max = " + max + ", min = " + min + ", count = " + count + "]";
	}


}
