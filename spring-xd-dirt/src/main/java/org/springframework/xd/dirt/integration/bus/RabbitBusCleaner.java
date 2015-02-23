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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 *
 * @author Gary Russell
 */
public class RabbitBusCleaner implements BusCleaner {

	final static Log logger = LogFactory.getLog(RabbitBusCleaner.class);

	@Override
	public List<String> clean(String stream) {
		return clean("http://localhost:15672", "guest", "guest", "/", "xdbus.", stream);
	}

	@SuppressWarnings("unchecked")
	public List<String> clean(String adminUri, String user, String pw, String vhost,
			String busPrefix, String stream) {
		List<String> results = new ArrayList<>();
		RestTemplate restTemplate = buildRestTemplate(adminUri, user, pw);
		int n = 0;
		while (true) {
			String queueName = busPrefix + stream + "." + n++;
			URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api").pathSegment("queues", "{vhost}", "{stream}")
					.buildAndExpand(vhost, queueName).encode().toUri();
			try {
				Map<String, Object> queue = restTemplate.getForObject(uri, Map.class);
				if (queue.get("consumers") != Integer.valueOf(0)) {
					throw new RabbitAdminException("Queue " + queueName + " is in use");
				}
				results.add(queueName);
				queueName += ".dlq";
				uri = UriComponentsBuilder.fromUriString(adminUri + "/api").pathSegment("queues", "{vhost}", "{stream}")
						.buildAndExpand(vhost, queueName).encode().toUri();
				try {
					queue = restTemplate.getForObject(uri, Map.class);
					if (queue.get("consumers") != Integer.valueOf(0)) {
						throw new RabbitAdminException("Queue " + queueName + " is in use");
					}
					results.add(queueName);
				}
				catch (HttpClientErrorException e) {
					if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
						continue; // DLQs are not mandatory
					}
					throw new RabbitAdminException("Failed to lookup queue", e);
				}
			}
			catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					break; // No more for this stream
				}
				throw new RabbitAdminException("Failed to lookup queue", e);
			}
		}
		for (String queueName : results) {
			URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api").pathSegment("queues", "{vhost}", "{stream}")
					.buildAndExpand(vhost, queueName).encode().toUri();
			restTemplate.delete(uri);
			if (logger.isDebugEnabled()) {
				logger.debug("deleted queue: " + queueName);
			}
		}
		return results;
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
