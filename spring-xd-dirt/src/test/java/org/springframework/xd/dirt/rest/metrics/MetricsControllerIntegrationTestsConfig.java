package org.springframework.xd.dirt.rest.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryGaugeRepository;

/**
 * @author Luke Taylor
 */
@Configuration
public class MetricsControllerIntegrationTestsConfig {

	@Bean
	public GaugeRepository gaugeRepository() {
		return new InMemoryGaugeRepository();
	}
}
