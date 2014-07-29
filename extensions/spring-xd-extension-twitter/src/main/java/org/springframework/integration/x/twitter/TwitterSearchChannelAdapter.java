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

import groovy.json.JsonSlurper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.social.support.URIBuilder;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Message producer which reads form Twitter's public search endpoints.
 * Reads from <a href="https://api.twitter.com/1.1/search/tweets.json"/>
 * The available parameters map directly to those defined for the
 * <a href="https://dev.twitter.com/docs/api/1.1/get/search/tweets">public search</a> API.
 *
 * @author Mark Fisher
 * @author Luke Taylor
 * @author David Turanski
 */
public class TwitterSearchChannelAdapter extends AbstractTwitterInboundChannelAdapter {

	public static enum ResultType {
		mixed, recent, popular
	};

	private static final String API_URL_BASE = "https://api.twitter.com/1.1/search/tweets.json";

	private String query;

	private String geocode;

	private ResultType resultType = ResultType.mixed;

	private Long sinceId;

	private boolean includeEntities = true;

	public TwitterSearchChannelAdapter(TwitterTemplate twitter) {
		super(twitter);
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setGeocode(String geocode) {
		this.geocode = geocode;
	}

	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}

	public void setIncludeEntities(boolean includeEntities) {
		this.includeEntities = includeEntities;
	}


	@Override
	public String getComponentType() {
		return "twitter:search-channel-adapter";
	}


	@Override
	protected URI buildUri() {

		URIBuilder b = URIBuilder.fromUri(API_URL_BASE);


		b.queryParam("q", query);


		if (!includeEntities) {
			b.queryParam("include_entities", "false");
		}

		if (!resultType.equals(ResultType.mixed)) {
			b.queryParam("result_type", resultType.toString());
		}

		if (StringUtils.hasText(getLanguage())) {
			b.queryParam("lang", getLanguage());
		}


		if (StringUtils.hasText(geocode)) {
			b.queryParam("geocode", geocode);
		}

		if (sinceId != null) {
			b.queryParam("since_id", String.valueOf(sinceId));
		}

		URI uri = b.build();
		if (logger.isDebugEnabled()) {
			logger.debug("Search uri:" + uri);
		}
		return uri;
	}

	/*
	 * Unwrap and split the search results into individual tweets. Advance the cursor ("since_id") for the next query.
	 * To avoid rate limit errors, wait at least 2 seconds (maximum rate according to twitter is 450/15 min or once every 2 seconds).
	 * If no data
	 * is returned, wait 10 seconds.
	 */
	@Override
	protected void doSendLine(String line) {
		ObjectMapper mapper = new ObjectMapper();
		JsonSlurper jsonSlurper = new JsonSlurper();
		@SuppressWarnings("unchecked")
		Map<String, List<Map<String, ?>>> map = (Map<String, List<Map<String, ?>>>) jsonSlurper.parseText(line);
		List<Map<String, ?>> statuses = map.get("statuses");
		for (Map<String, ?> tweet : statuses) {
			StringWriter sw = new StringWriter();
			try {
				mapper.writeValue(sw, tweet);
				sendMessage(MessageBuilder.withPayload(sw.toString()).build());
			}
			catch (JsonGenerationException ex) {
				logger.error("Failed to convert tweet to json: " + ex.getMessage());
				break;
			}
			catch (JsonMappingException ex) {
				logger.error("Failed to convert tweet to json: " + ex.getMessage());
				break;
			}
			catch (IOException ex) {
				logger.error("Failed to convert tweet to json: " + ex.getMessage());
				break;
			}
		}
		if (!CollectionUtils.isEmpty(statuses)) {
			this.sinceId = ((Long) (statuses.get(statuses.size() - 1).get("id")));
			wait(2100);
		}
		else {
			wait(10000);
		}
	}


}
