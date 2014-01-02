/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.plugins.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.core.Module;

/**
 * @author David Turanski
 * @author Gary Russell
 * @since 1.0
 */
public class ContentTypeTests {

	@Mock
	private MessageBus bus;

	private StreamPlugin streamPlugin = new StreamPlugin();

	@Mock
	private Module module;

	private DeploymentMetadata deploymentMetadata = new DeploymentMetadata("mystream", 1);

	private MessageChannel input = new DirectChannel();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		when(module.getDeploymentMetadata()).thenReturn(deploymentMetadata);
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAcceptedMediaTypes() {
		when(module.getComponent("accepted-content-types", Collection.class)).thenReturn(
				Collections.singletonList("application/json"));
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.APPLICATION_JSON,
						((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class),
				anyBoolean());
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class), anyBoolean());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAcceptedMediaTypesMixed() {
		List<Object> types = new ArrayList<Object>();
		types.add("application/json");
		types.add(MediaType.APPLICATION_XML);
		when(module.getComponent("accepted-content-types", Collection.class)).thenReturn(types);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Collection<MediaType> types = (Collection<MediaType>) invocation.getArguments()[2];
				assertEquals(2, types.size());
				Iterator<MediaType> iterator = types.iterator();
				MediaType first = iterator.next();
				assertTrue(first.equals(MediaType.APPLICATION_JSON) || first.equals(MediaType.APPLICATION_XML));
				MediaType second = iterator.next();
				assertTrue(second.equals(MediaType.APPLICATION_JSON) || second.equals(MediaType.APPLICATION_XML));
				assertFalse(first.equals(second));
				return null;
			}
		}).when(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class),
				anyBoolean());
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class), anyBoolean());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAcceptsAllMediaTypesByDefault() {
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.ALL, ((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class),
				anyBoolean());
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class), anyBoolean());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyMediaTypesAcceptsAll() {
		when(module.getComponent("accepted-media-types", Collection.class)).thenReturn(Arrays.asList(new String[0]));
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.ALL, ((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class),
				anyBoolean());
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer(anyString(), any(MessageChannel.class), any(Collection.class), anyBoolean());
	}

}
