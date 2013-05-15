package org.springframework.xd.analytics.metrics.memory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.xd.analytics.metrics.SharedFieldValueCounterRepositoryTests;

public class InMemoryFieldValueCounterRepositoryTests extends SharedFieldValueCounterRepositoryTests {

	@AfterClass
	@BeforeClass
	public static void beforeAndAfter() {
		fvRepository = new InMemoryFieldValueCounterRepository();
	}
}
