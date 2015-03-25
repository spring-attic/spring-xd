/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server.admin.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;


/**
 * Tests for {@link ContainerMatcher}
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultContainerMatcherTests {

	@Mock
	private ContainerRepository containerRepository;

	private ContainerMatcher containerMatcher = new ContainerMatcher();

	private List<Container> containers = new ArrayList<Container>();

	private ModuleDefinition moduleDefinition;

	private ModuleDeploymentProperties deploymentProperties;

	private ModuleDescriptor moduleDescriptor;

	@Before
	public void setUp() {
		moduleDefinition = TestModuleDefinitions.dummy("foo", ModuleType.processor);
		deploymentProperties = new ModuleDeploymentProperties();
		moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(moduleDefinition)
				.setGroup("test1")
				.setModuleLabel("amodule")
				.setIndex(0)
				.build();

		Map<String, String> container1Attributes = new HashMap<String, String>();
		container1Attributes.put("group", "group1");
		container1Attributes.put("color", "green");
		final Container container1 = new Container("container1", container1Attributes);

		Map<String, String> container2Attributes = new HashMap<String, String>();
		container2Attributes.put("group", "group2");
		container2Attributes.put("color", "green");
		container2Attributes.put("size", "large");

		final Container container2 = new Container("container2", container2Attributes);

		Map<String, String> container3Attributes = new HashMap<String, String>();
		container3Attributes.put("group", "group2");
		container3Attributes.put("color", "blue");
		container3Attributes.put("size", "small");

		final Container container3 = new Container("container3", container3Attributes);

		containers.add(container1);
		containers.add(container2);
		containers.add(container3);
	}

	@Test
	public void basicRoundRobin() {
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		assertSame(containers.get(0), matched.iterator().next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		assertSame(containers.get(1), matched.iterator().next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		assertSame(containers.get(2), matched.iterator().next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		assertSame(containers.get(0), matched.iterator().next());
	}

	@Test
	public void matchWithCountLessThanNumberOfContainers() {
		deploymentProperties.setCount(2);
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		Iterator<Container> matchedIterator = matched.iterator();
		assertSame(containers.get(0), matchedIterator.next());
		assertSame(containers.get(1), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(2), matchedIterator.next());
		assertSame(containers.get(0), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(1), matchedIterator.next());
		assertSame(containers.get(2), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(0), matchedIterator.next());
		assertSame(containers.get(1), matchedIterator.next());
	}

	@Test
	public void matchWithCountEqualToNumberOfContainers() {
		deploymentProperties.setCount(3);
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(3, matched.size());
	}

	@Test
	public void matchWithZeroCount() {
		deploymentProperties.setCount(0);
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(3, matched.size());
	}

	@Test
	public void matchWithCountGreaterThanToNumberOfContainers() {
		deploymentProperties.setCount(5);
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(3, matched.size());
	}


	@Test
	public void matchWithCriteria() {
		deploymentProperties.setCount(0);
		deploymentProperties.setCriteria("color=='green'");
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		Iterator<Container> matchedIterator = matched.iterator();
		assertSame(containers.get(0), matchedIterator.next());
		assertSame(containers.get(1), matchedIterator.next());
	}

	@Test
	public void matchWithCountAndCriteria() {
		deploymentProperties.setCount(3);
		deploymentProperties.setCriteria("group=='group2'");
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		Iterator<Container> matchedIterator = matched.iterator();
		assertSame(containers.get(1), matchedIterator.next());
		assertSame(containers.get(2), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(2, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(1), matchedIterator.next());
		assertSame(containers.get(2), matchedIterator.next());
	}

	@Test
	public void matchWithCountLessThanContainersMatchingCriteria() {
		deploymentProperties.setCount(1);
		deploymentProperties.setCriteria("group=='group2'");
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		Iterator<Container> matchedIterator = matched.iterator();
		assertSame(containers.get(1), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(2), matchedIterator.next());

		matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(1, matched.size());
		matchedIterator = matched.iterator();
		assertSame(containers.get(1), matchedIterator.next());
	}

	@Test
	public void matchWithNonExistentCriteria() {

		deploymentProperties.setCount(0);
		deploymentProperties.setCriteria("foo=='bar'");
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(0, matched.size());
	}

	@Test
	public void matchWithInvalidCriteria() {
		deploymentProperties.setCount(0);
		deploymentProperties.setCriteria("color");
		Collection<Container> matched = containerMatcher.match(moduleDescriptor, deploymentProperties,
				containers);
		assertEquals(0, matched.size());
	}
}
