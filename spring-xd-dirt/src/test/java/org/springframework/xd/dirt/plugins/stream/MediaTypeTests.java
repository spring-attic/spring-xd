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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.http.MediaType;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;

import scala.actors.threadpool.Arrays;

/**
 * @author David Turanski
 * @author Gary Russell
 * @since 1.0
 *
 */
public class MediaTypeTests {

	@Mock
	private ChannelRegistry registry;

	private StreamPlugin streamPlugin = new StreamPlugin();

	@Mock
	private Module module;

	private DeploymentMetadata deploymentMetadata = new DeploymentMetadata("mystream", 1);

	private MessageChannel input = new DirectChannel();

	private MessageChannel output = new DirectChannel();

	private static final Collection<MediaType> ALL = Collections.singletonList(MediaType.ALL);

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAcceptedMediaTypes() {
		when(module.getComponent("accepted-media-types", Collection.class)).thenReturn(Collections.singletonList("application/json"));
		streamPlugin.postProcessModule(module);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.APPLICATION_JSON, ((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(registry).inbound(anyString(), any(MessageChannel.class), any(Collection.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAcceptsAllMediaTypesByDefault() {
		streamPlugin.postProcessModule(module);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.ALL, ((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(registry).inbound(anyString(), any(MessageChannel.class), any(Collection.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyMediaTypesAcceptsAll() {
		when(module.getComponent("accepted-media-types", Collection.class)).thenReturn(Arrays.asList(new String[0]));
		streamPlugin.postProcessModule(module);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(MediaType.ALL, ((Collection<?>) invocation.getArguments()[2]).iterator().next());
				return null;
			}
		}).when(registry).inbound(anyString(), any(MessageChannel.class), any(Collection.class));
	}

}
