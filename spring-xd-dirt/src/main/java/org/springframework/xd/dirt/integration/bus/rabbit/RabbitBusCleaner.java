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

package org.springframework.xd.dirt.integration.bus.rabbit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.xd.dirt.integration.bus.BusCleaner;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.plugins.AbstractJobPlugin;
import org.springframework.xd.dirt.plugins.AbstractMessageBusBinderPlugin;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.dirt.plugins.job.JobEventsListenerPlugin;

import com.google.common.annotations.VisibleForTesting;


/**
 * Implementation of {@link BusCleaner} for the {@code RabbitMessageBus}.
 *
 * @author Gary Russell
 * @since 1.2
 */
public class RabbitBusCleaner implements BusCleaner {

	private final static Log logger = LogFactory.getLog(RabbitBusCleaner.class);

	@Override
	public Map<String, List<String>> clean(String entity, boolean isJob) {
		return clean("http://localhost:15672", "guest", "guest", "/", "xdbus.", entity, isJob);
	}

	public Map<String, List<String>> clean(String adminUri, String user, String pw, String vhost,
			String busPrefix, String entity, boolean isJob) {
		return doClean(
				adminUri == null ? "http://localhost:15672" : adminUri,
				user == null ? "guest" : user,
				pw == null ? "guest" : pw,
				vhost == null ? "/" : vhost,
				busPrefix == null ? "xdbus." : busPrefix,
				entity, isJob);
	}

	private Map<String, List<String>> doClean(String adminUri, String user, String pw, String vhost,
			String busPrefix, String entity, boolean isJob) {
		RestTemplate restTemplate = buildRestTemplate(adminUri, user, pw);
		List<String> removedQueues = isJob
				? findJobQueues(adminUri, vhost, busPrefix, entity, restTemplate)
				: findStreamQueues(adminUri, vhost, busPrefix, entity, restTemplate);
		ExchangeCandidateCallback callback;
		if (isJob) {
			Collection<String> exchangeNames = JobEventsListenerPlugin.getEventListenerChannels(entity).values();
			final Set<String> jobExchanges = new HashSet<>();
			for (String exchange : exchangeNames) {
				jobExchanges.add(MessageBusSupport.applyPrefix(busPrefix, MessageBusSupport.applyPubSub(exchange)));
			}
			jobExchanges.add(MessageBusSupport.applyPrefix(busPrefix, MessageBusSupport.applyPubSub(
					JobEventsListenerPlugin.getEventListenerChannelName(entity))));
			callback = new ExchangeCandidateCallback() {

				@Override
				public boolean isCandidate(String exchangeName) {
					return jobExchanges.contains(exchangeName);
				}

			};
		}
		else {
			final String tapPrefix = MessageBusSupport.applyPrefix(busPrefix,
					MessageBusSupport.applyPubSub(AbstractStreamPlugin.constructTapPrefix(entity)));
			callback = new ExchangeCandidateCallback() {

				@Override
				public boolean isCandidate(String exchangeName) {
					return exchangeName.startsWith(tapPrefix);
				}
			};
		}
		List<String> removedExchanges = findExchanges(adminUri, vhost, busPrefix, entity, restTemplate, callback);
		// Delete the queues in reverse order to enable re-running after a partial success.
		// The queue search above starts with 0 and terminates on a not found.
		for (int i = removedQueues.size() - 1; i >= 0; i--) {
			String queueName = removedQueues.get(i);
			URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
					.pathSegment("queues", "{vhost}", "{stream}")
					.buildAndExpand(vhost, queueName).encode().toUri();
			restTemplate.delete(uri);
			if (logger.isDebugEnabled()) {
				logger.debug("deleted queue: " + queueName);
			}
		}
		Map<String, List<String>> results = new HashMap<>();
		if (removedQueues.size() > 0) {
			results.put("queues", removedQueues);
		}
		// Fanout exchanges for taps
		for (String exchange : removedExchanges) {
			URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
					.pathSegment("exchanges", "{vhost}", "{name}")
					.buildAndExpand(vhost, exchange).encode().toUri();
			restTemplate.delete(uri);
			if (logger.isDebugEnabled()) {
				logger.debug("deleted exchange: " + exchange);
			}
		}
		if (removedExchanges.size() > 0) {
			results.put("exchanges", removedExchanges);
		}
		return results;
	}

	private List<String> findStreamQueues(String adminUri, String vhost, String busPrefix, String stream,
			RestTemplate restTemplate) {
		List<String> removedQueues = new ArrayList<>();
		int n = 0;
		while (true) { // exits when no queue found
			String queueName = MessageBusSupport.applyPrefix(busPrefix,
					AbstractMessageBusBinderPlugin.constructPipeName(stream, n++));
			URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
					.pathSegment("queues", "{vhost}", "{stream}")
					.buildAndExpand(vhost, queueName).encode().toUri();
			try {
				getQueueDetails(restTemplate, queueName, uri);
				removedQueues.add(queueName);
			}
			catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					break; // No more for this stream
				}
				throw new RabbitAdminException("Failed to lookup queue " + queueName, e);
			}
			queueName = MessageBusSupport.constructDLQName(queueName);
			uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
					.pathSegment("queues", "{vhost}", "{stream}")
					.buildAndExpand(vhost, queueName).encode().toUri();
			try {
				getQueueDetails(restTemplate, queueName, uri);
				removedQueues.add(queueName);
			}
			catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					continue; // DLQs are not mandatory
				}
				throw new RabbitAdminException("Failed to lookup queue " + queueName, e);
			}
		}
		return removedQueues;
	}

	private List<String> findJobQueues(String adminUri, String vhost, String busPrefix, String job,
			RestTemplate restTemplate) {
		List<String> removedQueues = new ArrayList<>();
		String jobQueueName = MessageBusSupport.applyPrefix(busPrefix, AbstractJobPlugin.getJobChannelName(job));
		URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
				.pathSegment("queues", "{vhost}", "{job}")
				.buildAndExpand(vhost, jobQueueName).encode().toUri();
		try {
			getQueueDetails(restTemplate, jobQueueName, uri);
			removedQueues.add(jobQueueName);
		}
		catch (HttpClientErrorException e) {
			if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				throw new RabbitAdminException("Failed to lookup queue " + jobQueueName, e);
			}
		}
		String jobRequestsQueueName = MessageBusSupport.applyPrefix(busPrefix,
				MessageBusSupport.applyRequests(AbstractMessageBusBinderPlugin.constructPipeName(
						AbstractJobPlugin.getJobChannelName(job), 0)));
		uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
				.pathSegment("queues", "{vhost}", "{job}")
				.buildAndExpand(vhost, jobRequestsQueueName).encode().toUri();
		try {
			getQueueDetails(restTemplate, jobRequestsQueueName, uri);
			removedQueues.add(jobRequestsQueueName);
		}
		catch (HttpClientErrorException e) {
			if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				throw new RabbitAdminException("Failed to lookup queue " + jobRequestsQueueName, e);
			}
		}
		return removedQueues;
	}

	@SuppressWarnings("unchecked")
	private void getQueueDetails(RestTemplate restTemplate, String queueName, URI uri) {
		Map<String, Object> queue = restTemplate.getForObject(uri, Map.class);
		if (queue.get("consumers") != Integer.valueOf(0)) {
			throw new RabbitAdminException("Queue " + queueName + " is in use");
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> findExchanges(String adminUri, String vhost, String busPrefix, String entity,
			RestTemplate restTemplate, ExchangeCandidateCallback callback) {
		List<String> removedExchanges = new ArrayList<>();
		URI uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
				.pathSegment("exchanges", "{vhost}")
				.buildAndExpand(vhost).encode().toUri();
		List<Map<String, Object>> exchanges = restTemplate.getForObject(uri, List.class);
		for (Map<String, Object> exchange : exchanges) {
			String exchangeName = (String) exchange.get("name");
			if (callback.isCandidate(exchangeName)) {
				uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
						.pathSegment("exchanges", "{vhost}", "{name}", "bindings", "source")
						.buildAndExpand(vhost, exchangeName).encode().toUri();
				List<Map<String, Object>> bindings = restTemplate.getForObject(uri, List.class);
				if (bindings.size() == 0) {
					uri = UriComponentsBuilder.fromUriString(adminUri + "/api")
							.pathSegment("exchanges", "{vhost}", "{name}", "bindings", "destination")
							.buildAndExpand(vhost, exchangeName).encode().toUri();
					bindings = restTemplate.getForObject(uri, List.class);
					if (bindings.size() == 0) {
						removedExchanges.add((String) exchange.get("name"));
					}
					else {
						throw new RabbitAdminException("Cannot delete exchange " + exchangeName
								+ "; it is a destination: " + bindings);
					}
				}
				else {
					throw new RabbitAdminException("Cannot delete exchange " + exchangeName + "; it has bindings: "
							+ bindings);
				}
			}
		}
		return removedExchanges;
	}

	private interface ExchangeCandidateCallback {

		boolean isCandidate(String exchangeName);
	}

	@VisibleForTesting
	static RestTemplate buildRestTemplate(String adminUri, String user, String password) {
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
