/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.tuple;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

/**
 * Illustrates the impact of creating a {@link DefaultTupleConversionService} for each
 * instance of Tuple.  The recommended approach is to either use the default instance or
 * configure a customized version that is created as a singleton within the context of
 * the module it's used.
 * 
 * @author Michael Minella
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TupleCreationPerformanceTests.ConversionServiceConfiguration.class})
public class TupleCreationPerformanceTests {

	@Configuration
	static class ConversionServiceConfiguration {

		@Bean
		public FormattingConversionService conversionService() {
			return new DefaultTupleConversionService();
		}
	}

	@Autowired
	private FormattingConversionService conversionService;

	@Test
	@Repeat(4)
	public void testCreationOfTuples() {
		StopWatch watch = new StopWatch("simple");
		watch.start();
		for(int i = 0; i < 100000; i++) {
			TupleBuilder.tuple()
				.put("field1", Math.random())
				.put("field2", UUID.randomUUID())
				.put("field3", "AAAAA")
				.put("field4", new Date().getTime()).build();
		}
		watch.stop();
		System.out.println(watch.prettyPrint());
	}

	@Test
	@Repeat(4)
	public void testCreationOfTuplesCustomConversionService() {
		StopWatch watch = new StopWatch("customConversionService");
		watch.start();
		for(int i = 0; i < 100000; i++) {
			TupleBuilder.tuple()
				.setConfigurableConversionService(new DefaultTupleConversionService())
				.put("field1", Math.random())
				.put("field2", UUID.randomUUID())
				.put("field3", "AAAAA")
				.put("field4", new Date().getTime()).build();
		}
		watch.stop();
		System.out.println(watch.prettyPrint());
	}

	@Test
	@Repeat(4)
	public void testCreationOfTuplesCustomConversionServiceAutowired() {
		StopWatch watch = new StopWatch("customcConversionServiceAutowired");
		watch.start();
		for(int i = 0; i < 100000; i++) {
			TupleBuilder.tuple()
				.setConfigurableConversionService(conversionService)
				.put("field1", Math.random())
				.put("field2", UUID.randomUUID())
				.put("field3", "AAAAA")
				.put("field4", new Date().getTime()).build();
		}
		watch.stop();
		System.out.println(watch.prettyPrint());
	}
}
