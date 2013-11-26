/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.stream.memory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;

/**
 * Unit test for InMemoryStreamDefinitionRepository.
 * 
 * @author Eric Bottard
 */
public class InMemoryStreamDefinitionRepositoryTests {

	private InMemoryStreamDefinitionRepository repository = new InMemoryStreamDefinitionRepository(
			new InMemoryModuleDependencyRepository());

	@Test
	public void newlyCreatedRepo() {
		Assert.assertEquals(0, repository.count());
		Assert.assertNull(repository.findOne("some"));
		Assert.assertFalse(repository.findAll().iterator().hasNext());
		Assert.assertFalse(repository.exists("some"));
	}

	@Test
	public void simpleAdding() {
		StreamDefinition stream = new StreamDefinition("some", "http | hdfs");
		repository.save(stream);

		Assert.assertTrue(repository.exists("some"));
		Assert.assertEquals(1, repository.count());
		Assert.assertNotNull(repository.findOne("some"));
		Iterator<StreamDefinition> iterator = repository.findAll().iterator();
		Assert.assertEquals(stream, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void multipleAdding() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		repository.save(Arrays.asList(one, two));

		Assert.assertTrue(repository.exists("one"));
		Assert.assertTrue(repository.exists("two"));
	}

	@Test
	public void multipleFinding() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		repository.save(one);

		Iterator<StreamDefinition> iterator = repository.findAll(Arrays.asList("one", "notthere")).iterator();
		Assert.assertEquals(one, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void deleteSimple() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		repository.save(Arrays.asList(one, two));

		repository.delete("one");
		Assert.assertFalse(repository.exists("one"));

		repository.delete(two);
		Assert.assertFalse(repository.exists("two"));
	}

	@Test
	public void deleteMultiple() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		StreamDefinition three = new StreamDefinition("three", "http | file");
		repository.save(Arrays.asList(one, two, three));

		repository.delete(Arrays.asList(one));
		Assert.assertFalse(repository.exists("one"));
		Assert.assertTrue(repository.exists("two"));

		repository.deleteAll();
		Assert.assertEquals(0, repository.count());
	}

	@Test
	public void override() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		repository.save(one);
		Assert.assertEquals("http | hdfs", repository.findOne("one").getDefinition());

		StreamDefinition otherone = new StreamDefinition("one", "time | file");
		repository.save(otherone);
		Assert.assertEquals(1, repository.count());
		Assert.assertEquals("time | file", repository.findOne("one").getDefinition());
	}

	@Test
	public void sorting() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		StreamDefinition three = new StreamDefinition("three", "http | file");
		StreamDefinition four = new StreamDefinition("four", "http | file");
		StreamDefinition five = new StreamDefinition("five", "http | file");
		repository.save(Arrays.asList(one, two, three, four, five));

		Assert.assertEquals(5, repository.count());

		PageRequest pageable = new PageRequest(1, 2);
		Page<StreamDefinition> sub = repository.findAll(pageable);

		Assert.assertEquals(5, sub.getTotalElements());
		Assert.assertEquals(1, sub.getNumber());
		Assert.assertEquals(2, sub.getNumberOfElements());
		Assert.assertEquals(2, sub.getSize());
		Assert.assertEquals(3, sub.getTotalPages());
		List<StreamDefinition> content = sub.getContent();
		Assert.assertEquals("one", content.get(0).getName());
		Assert.assertEquals("three", content.get(1).getName());

	}
}
