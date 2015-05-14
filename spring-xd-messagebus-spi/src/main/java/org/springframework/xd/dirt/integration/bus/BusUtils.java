/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Message Bus utilities.
 *
 * @author Gary Russell
 */
public class BusUtils {

	public static final String TAP_CHANNEL_PREFIX = "tap:";

	public static final String TOPIC_CHANNEL_PREFIX = "topic:";

	public static final Pattern PUBSUB_NAMED_CHANNEL_PATTERN = Pattern.compile("[^.]+\\.(tap|topic):");

	public static String addGroupToPubSub(String group, String inputChannelName) {
		if (inputChannelName.startsWith(TAP_CHANNEL_PREFIX)
				|| inputChannelName.startsWith(TOPIC_CHANNEL_PREFIX)) {
			inputChannelName = group + "." + inputChannelName;
		}
		return inputChannelName;
	}

	public static String removeGroupFromPubSub(String name) {
		if (PUBSUB_NAMED_CHANNEL_PATTERN.matcher(name).find()) {
			return name.substring(name.indexOf(".") + 1);
		}
		else {
			return name;
		}
	}

	/**
	 * Determine whether the provided channel name represents a pub/sub channel (i.e. topic or tap).
	 * @param channelName name of the channel to check
	 * @return true if pub/sub.
	 */
	public static boolean isChannelPubSub(String channelName) {
		Assert.isTrue(StringUtils.hasText(channelName), "Channel name should not be empty/null.");
		// Check if the channelName starts with tap: or topic:
		return (channelName.startsWith(TAP_CHANNEL_PREFIX) || channelName.startsWith(TOPIC_CHANNEL_PREFIX));
	}

	/**
	 * Construct a pipe name from the group and index.
	 * @param group the group.
	 * @param index the index.
	 * @return the name.
	 */
	public static String constructPipeName(String group, int index) {
		return group + "." + index;
	}

	public static String constructTapPrefix(String group) {
		return TAP_CHANNEL_PREFIX + "stream:" + group;
	}

	public static RestTemplate buildRestTemplate(String adminUri, String user, String password) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, password));
		HttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		// Set up pre-emptive basic Auth because the rabbit plugin doesn't currently support challenge/response for PUT
		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local; from the apache docs...
		// auth cache
		BasicScheme basicAuth = new BasicScheme();
		URI uri;
		try {
			uri = new URI(adminUri);
		}
		catch (URISyntaxException e) {
			throw new RabbitAdminException("Invalid URI", e);
		}
		authCache.put(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), basicAuth);
		// Add AuthCache to the execution context
		final HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient) {

			@Override
			protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
				return localContext;
			}

		});
		restTemplate.setMessageConverters(Collections.<HttpMessageConverter<?>> singletonList(
				new MappingJackson2HttpMessageConverter()));
		return restTemplate;
	}

}
