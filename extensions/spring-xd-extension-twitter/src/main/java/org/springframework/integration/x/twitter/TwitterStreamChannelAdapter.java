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

import java.net.URI;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.social.support.URIBuilder;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.StringUtils;

/**
 * Message producer which reads form Twitter's public stream endpoints.
 *
 * Unless filtering parameters are set, it will read from the <tt>statuses/sample.json</tt> endpoint, but if any of the
 * <tt>track</tt>, <tt>follow</tt> or <tt>locations</tt> parameters are set, it will switch to using
 * <tt>statuses/filter.json</tt>.
 *
 * The available parameters map directly to those defined for the <a
 * href="https://dev.twitter.com/docs/streaming-apis/streams/public">public streams</a> API.
 *
 * @author Mark Fisher
 * @author Luke Taylor
 * @author David Turanski
 */
public class TwitterStreamChannelAdapter extends AbstractTwitterInboundChannelAdapter {

	private static final String API_URL_BASE = "https://stream.twitter.com/1.1/";

	public static enum FilterLevel {
		none, low, medium
	};

	private boolean delimited;

	private boolean stallWarnings;

	private FilterLevel filterLevel = FilterLevel.none;

	private String track = "";

	private String follow = "";

	private String locations = "";

	public TwitterStreamChannelAdapter(TwitterTemplate twitter) {
		super(twitter);
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
	public void setFilterLevel(FilterLevel filterLevel) {
		this.filterLevel = filterLevel;
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
	protected URI buildUri() {
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

		if (!FilterLevel.none.equals(filterLevel)) {
			b.queryParam("filter_level", filterLevel.toString());
		}

		if (StringUtils.hasText(getLanguage())) {
			b.queryParam("language", getLanguage());
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

	@Override
	protected void doSendLine(String line) {
		sendMessage(MessageBuilder.withPayload(line).build());
	}

}
