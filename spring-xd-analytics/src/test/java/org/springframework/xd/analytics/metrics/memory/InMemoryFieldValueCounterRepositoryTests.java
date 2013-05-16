package org.springframework.xd.analytics.metrics.memory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.xd.analytics.metrics.SharedFieldValueCounterRepositoryTests;

public class InMemoryFieldValueCounterRepositoryTests extends SharedFieldValueCounterRepositoryTests {

	@After
	@Before
	public void beforeAndAfter() {
		fvRepository = new InMemoryFieldValueCounterRepository();
	}
}
