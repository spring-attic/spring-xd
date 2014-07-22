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

package org.springframework.integration.x.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;


/**
 *
 * @author David Turanski
 * @author Gary Russell
 */
public abstract class AbstractTwitterInboundChannelAdapter extends MessageProducerSupport {

	private final static AtomicInteger instance = new AtomicInteger();

	private final TwitterTemplate twitter;

	private final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

	private String language = "";

	private final Object monitor = new Object();

	private final AtomicBoolean running = new AtomicBoolean(false);

	// Backoff values, as per https://dev.twitter.com/docs/streaming-apis/connecting#Reconnecting
	private final AtomicInteger linearBackOff = new AtomicInteger(250);

	private final AtomicInteger httpErrorBackOff = new AtomicInteger(5000);

	private final AtomicInteger rateLimitBackOff = new AtomicInteger(60000);

	protected AbstractTwitterInboundChannelAdapter(TwitterTemplate twitter) {
		this.twitter = twitter;
		// Fix to get round TwitterErrorHandler not handling 401s etc.
		this.twitter.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler());
		this.setPhase(Integer.MAX_VALUE);
	}

	/**
	 * The read timeout for the underlying URLConnection to the twitter stream.
	 */
	public void setReadTimeout(int millis) {
		// Hack to get round Spring's dynamic loading of http client stuff
		ClientHttpRequestFactory f = getRequestFactory();
		if (f instanceof SimpleClientHttpRequestFactory) {
			((SimpleClientHttpRequestFactory) f).setReadTimeout(millis);
		}
		else {
			((HttpComponentsClientHttpRequestFactory) f).setReadTimeout(millis);
		}
	}

	/**
	 * The connection timeout for making a connection to Twitter.
	 */
	public void setConnectTimeout(int millis) {
		ClientHttpRequestFactory f = getRequestFactory();
		if (f instanceof SimpleClientHttpRequestFactory) {
			((SimpleClientHttpRequestFactory) f).setConnectTimeout(millis);
		}
		else {
			((HttpComponentsClientHttpRequestFactory) f).setConnectTimeout(millis);
		}
	}

	/**
	 * Filter tweets by language or languages.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return this.language;
	}

	@Override
	protected void onInit() {
		this.taskExecutor.setThreadNamePrefix("twitterSource-" + instance.incrementAndGet() + "-");
		this.taskExecutor.initialize();
	}

	@Override
	protected void doStart() {
		synchronized (this.monitor) {
			if (this.running.get()) {
				// already running
				return;
			}
			this.running.set(true);
			this.taskExecutor.execute(new StreamReadingTask());
		}
	}

	@Override
	protected void doStop() {
		this.running.set(false);
		this.taskExecutor.getThreadPoolExecutor().shutdownNow();
		try {
			if (!this.taskExecutor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS)) {
				logger.error("Reader task failed to stop");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}


	protected abstract URI buildUri();

	protected abstract void doSendLine(String line);

	protected class StreamReadingTask implements Runnable {

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
					else if (sce.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
						waitRateLimitBackoff();
					}
					else {
						waitHttpErrorBackoff();
					}
				}
				catch (Exception e) {
					logger.warn("Exception while reading stream.", e);
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
									doSendLine(line);
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

	private ClientHttpRequestFactory getRequestFactory() {
		// InterceptingClientHttpRequestFactory doesn't let us access the underlying object
		DirectFieldAccessor f = new DirectFieldAccessor(twitter.getRestTemplate().getRequestFactory());
		Object requestFactory = f.getPropertyValue("requestFactory");
		return (ClientHttpRequestFactory) requestFactory;
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

	protected void wait(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (!this.running.get()) {
				// no longer running
				return;
			}
			throw new IllegalStateException(e);
		}
	}

}
