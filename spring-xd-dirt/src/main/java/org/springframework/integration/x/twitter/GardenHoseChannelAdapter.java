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

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.social.support.URIBuilder;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.*;

/**
 * Message producer for Twitter "Gardenhose" source.
 *
 * Invokes the Twitter streaming API.
 *
 * @author Mark Fisher
 * @author Luke Taylor
 */
public class GardenHoseChannelAdapter extends MessageProducerSupport {

	private static final String API_URL_BASE = "https://stream.twitter.com/1.1/";

	private static final LinkedMultiValueMap<String, String> EMPTY_PARAMETERS = new LinkedMultiValueMap<String, String>();

	private final TwitterTemplate twitter;

	private ScheduledFuture<?> task;

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final Object monitor = new Object();

	public GardenHoseChannelAdapter(TwitterTemplate twitter) {
		this.twitter = twitter;
		// Fix to get round TwitterErrorHandler not handling 401s etc.
		twitter.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler());
		this.setPhase(Integer.MAX_VALUE);
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

	private URI buildUri(String path) {
		return URIBuilder.fromUri(API_URL_BASE + path).queryParams(EMPTY_PARAMETERS).build();
	}

	private class StreamReadingTask implements Runnable {
		public void run() {
			while (running.get()) {
				try {
					readStream(twitter.getRestTemplate());
				}
				catch (HttpClientErrorException ec) {
					if (ec.getStatusCode() == HttpStatus.UNAUTHORIZED) {
						logger.error("Twitter authentication failed: " + ec.getMessage());
						running.set(false);
					}
				}
				catch (Exception e) {
					logger.warn("Exception while reading stream; restarting. Add debug logging for exception trace.");
					if (logger.isDebugEnabled()) {
						logger.debug("Exception while reading stream.", e);
					}
				}
			}
		}

		private void readStream(RestTemplate restTemplate) {
			restTemplate.execute(buildUri("statuses/sample.json"), HttpMethod.GET, new RequestCallback() {
						public void doWithRequest(ClientHttpRequest request) throws IOException {
						}
					},
					new ResponseExtractor<String>() {
						public String extractData(ClientHttpResponse response) throws IOException {
							InputStream inputStream = response.getBody();
							LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
							while (running.get()) {
								String line = reader.readLine();
								if (!StringUtils.hasText(line)) {
									break;
								}
								sendMessage(MessageBuilder.withPayload(line).build());
							}
							return null;
						}
					});
		}
	}

}


