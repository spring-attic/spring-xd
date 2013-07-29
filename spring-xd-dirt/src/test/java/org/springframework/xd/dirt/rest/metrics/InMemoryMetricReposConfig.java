package org.springframework.xd.dirt.rest.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryGaugeRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryRichGaugeRepository;

/**
 * @author Luke Taylor
 */
@Configuration
public class InMemoryMetricReposConfig {

	@Bean
	public GaugeRepository gaugeRepository() {
		return new InMemoryGaugeRepository();
	}

	@Bean
	public RichGaugeRepository richGaugeRepository() {
		return new InMemoryRichGaugeRepository();
	}
}
