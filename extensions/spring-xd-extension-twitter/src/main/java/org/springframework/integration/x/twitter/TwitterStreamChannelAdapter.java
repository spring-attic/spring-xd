/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.integration.x.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.HttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.social.support.URIBuilder;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * Message producer which reads form Twitter's public stream endpoints.
 * 
 * Unless filtering parameters are set, it will read from the <tt>statuses/sample.json</tt> endpoint, but if any of the
 * <tt>track</tt>, <tt>follow</tt> or <tt>locations</tt> parameters are set, it will switch to using
 * <tt>statuses/filter.json</tt>.
 * 
 * Apart from the read and connection timeouts, the available parameters map directly to those defined for the <a
 * href="https://dev.twitter.com/docs/streaming-apis/streams/public">public streams</a> API.
 * 
 * @author Mark Fisher
 * @author Luke Taylor
 */
public class TwitterStreamChannelAdapter extends MessageProducerSupport {

	private static final String API_URL_BASE = "https://stream.twitter.com/1.1/";

	private final TwitterTemplate twitter;

	private ScheduledFuture<?> task;

	private final AtomicBoolean running = new AtomicBoolean(false);

	// Backoff values, as per https://dev.twitter.com/docs/streaming-apis/connecting#Reconnecting
	private final AtomicInteger linearBackOff = new AtomicInteger(250);

	private final AtomicInteger httpErrorBackOff = new AtomicInteger(5000);

	private final AtomicInteger rateLimitBackOff = new AtomicInteger(60000);

	private boolean delimited;

	private boolean stallWarnings;

	private String filterLevel = "none";

	private String language = "";

	private String track = "";

	private String follow = "";

	private String locations = "";

	private final Object monitor = new Object();

	public TwitterStreamChannelAdapter(TwitterTemplate twitter) {
		this.twitter = twitter;
		// Fix to get round TwitterErrorHandler not handling 401s etc.
		twitter.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler());
		this.setPhase(Integer.MAX_VALUE);
	}

	/**
	 * The read timeout for the underlying URLConnection to the twitter stream.
	 */
	public void setReadTimeout(int millis) {
		getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, millis);
	}

	/**
	 * The connection timeout for making a connection to Twitter.
	 */
	public void setConnectTimeout(int millis) {
		getHttpClient().getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, millis);
	}

	private HttpClient getHttpClient() {
		// InterceptingClientHttpRequestFactory doesn't let us access the underlying object
		DirectFieldAccessor f = new DirectFieldAccessor(twitter.getRestTemplate().getRequestFactory());
		// Unfortunately this class is internal to Spring Social - HttpComponentsClientHttpRequestFactory
		// Not to be confused with the Spring class of the same name.
		Object requestFactory = f.getPropertyValue("requestFactory");
		f = new DirectFieldAccessor(requestFactory);
		return (HttpClient) f.getPropertyValue("httpClient");
	}

	/**
	 * Whether "delimited=length" shoud be added to the query.
	 */
	public void setDelimited(boolean delimited) {
		this.delimited = delimited;
	}

	/**
	 * Whether "stall_warnings=true" should be added to the query.
	 */
	public void setStallWarnings(boolean stallWarnings) {
		this.stallWarnings = stallWarnings;
	}

	/**
	 * One of "none", "low" or "medium"
	 */
	public void setFilterLevel(String filterLevel) {
		this.filterLevel = filterLevel;
	}

	/**
	 * Filter tweets by language or languages.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Filter tweets by words or phrases.
	 */
	public void setTrack(String track) {
		this.track = track;
	}

	/**
	 * Restrict the stream to a user or users.
	 */
	public void setFollow(String follow) {
		this.follow = follow;
	}

	/**
	 * Bound the returned tweets by location(s).
	 */
	public void setLocations(String locations) {
		this.locations = locations;
	}

	@Override
	public String getComponentType() {
		return "twitter:gardenhose-channel-adapter";
	}

	@Override
	protected void doStart() {
		synchronized (this.monitor) {
			if (this.running.get()) {
				// already running
				return;
			}
			this.running.set(true);
			StreamReadingTask task = new StreamReadingTask();
			TaskScheduler scheduler = getTaskScheduler();
			if (scheduler != null) {
				this.task = scheduler.schedule(task, new Date());
			}
			else {
				Executor executor = Executors.newSingleThreadExecutor();
				executor.execute(task);
			}
		}
	}

	@Override
	protected void doStop() {
		if (this.task != null) {
			this.task.cancel(true);
			this.task = null;
		}
		this.running.set(false);
	}

	private URI buildUri() {
		String path = "statuses/sample.json";

		if (StringUtils.hasText(track) || StringUtils.hasText(follow) || StringUtils.hasText(locations)) {
			path = "statuses/filter.json";
		}

		URIBuilder b = URIBuilder.fromUri(API_URL_BASE + path);

		if (delimited) {
			b.queryParam("delimited", "length");
		}

		if (stallWarnings) {
			b.queryParam("stall_warnings", "true");
		}

		if (!"none".equals(filterLevel)) {
			b.queryParam("filter_level", filterLevel);
		}

		if (StringUtils.hasText(language)) {
			b.queryParam("language", language);
		}

		if (StringUtils.hasText(track)) {
			b.queryParam("track", track);
		}

		if (StringUtils.hasText(follow)) {
			b.queryParam("follow", follow);
		}

		if (StringUtils.hasText(locations)) {
			b.queryParam("locations", locations);
		}
		return b.build();
	}

	private void resetBackOffs() {
		linearBackOff.set(250);
		rateLimitBackOff.set(60000);
		httpErrorBackOff.set(5000);
	}

	private void waitLinearBackoff() {
		int millis = linearBackOff.get();
		logger.warn("Exception while reading stream, waiting for " + millis + " ms before restarting");
		wait(millis);
		if (millis < 16000) // 16 seconds max
			linearBackOff.set(millis + 250);
	}

	private void waitRateLimitBackoff() {
		int millis = rateLimitBackOff.get();
		logger.warn("Rate limit error, waiting for " + millis / 1000 + " seconds before restarting");
		wait(millis);
		rateLimitBackOff.set(millis * 2);
	}

	private void waitHttpErrorBackoff() {
		int millis = httpErrorBackOff.get();
		logger.warn("Http error, waiting for " + millis / 1000 + " seconds before restarting");
		wait(millis);
		if (millis < 320000) // 320 seconds max
			httpErrorBackOff.set(millis * 2);
	}

	private void wait(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
		}
	}

	@SuppressWarnings("deprecation")
	private class StreamReadingTask implements Runnable {

		@Override
		public void run() {
			while (running.get()) {
				try {
					readStream(twitter.getRestTemplate());
				}
				catch (HttpStatusCodeException sce) {
					if (sce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
						logger.error("Twitter authentication failed: " + sce.getMessage());
						running.set(false);
					}
					else if (sce.getStatusCode() == HttpStatus.METHOD_FAILURE) {
						waitRateLimitBackoff();
					}
					else {
						waitHttpErrorBackoff();
					}
				}
				catch (Exception e) {
					logger.warn("Exception while reading stream; Add debug logging for exception trace.");
					if (logger.isDebugEnabled()) {
						logger.debug("Exception while reading stream.", e);
					}
					waitLinearBackoff();
				}
			}
		}

		private void readStream(RestTemplate restTemplate) {
			restTemplate.execute(buildUri(), HttpMethod.GET, new RequestCallback() {

				@Override
				public void doWithRequest(ClientHttpRequest request) throws IOException {
				}
			},
					new ResponseExtractor<String>() {

						@Override
						public String extractData(ClientHttpResponse response) throws IOException {
							InputStream inputStream = response.getBody();
							LineNumberReader reader = null;
							try {
								reader = new LineNumberReader(new InputStreamReader(inputStream));
								resetBackOffs();
								while (running.get()) {
									String line = reader.readLine();
									if (!StringUtils.hasText(line)) {
										break;
									}
									sendMessage(MessageBuilder.withPayload(line).build());
								}
							}
							finally {
								if (reader != null) {
									reader.close();
								}
							}

							return null;
						}
					}
					);
		}
	}

}
