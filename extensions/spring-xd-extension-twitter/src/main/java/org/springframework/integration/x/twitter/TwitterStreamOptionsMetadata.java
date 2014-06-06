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

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.integration.x.twitter.TwitterStreamChannelAdapter.FilterLevel;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 *
 * @author David Turanski
 */
@Mixin({ TwitterMixin.class })
public class TwitterStreamOptionsMetadata {

	private boolean delimited;

	private boolean stallWarnings;

	private FilterLevel filterLevel = FilterLevel.none;

	private String track = "";

	private String follow = "";

	private String locations = "";

	private String accessToken;

	private String accessTokenSecret;

	private boolean discardDeletes = true;


	public boolean isDelimited() {
		return delimited;
	}

	@ModuleOption("set to true to get length delimiters in the stream data")
	public void setDelimited(boolean delimited) {
		this.delimited = delimited;
	}


	public boolean isStallWarnings() {
		return stallWarnings;
	}

	@ModuleOption("set to true to enable stall warnings")
	public void setStallWarnings(boolean stallWarnings) {
		this.stallWarnings = stallWarnings;
	}


	public FilterLevel getFilterLevel() {
		return filterLevel;
	}

	@ModuleOption("controls which tweets make it through to the stream: none,low,or medium")
	public void setFilterLevel(FilterLevel filterLevel) {
		this.filterLevel = filterLevel;
	}

	public String getTrack() {
		return track;
	}

	@ModuleOption("comma delimited set of terms to include in the stream")
	public void setTrack(String track) {
		this.track = track;
	}


	public String getFollow() {
		return follow;
	}

	@ModuleOption("comma delimited set of user ids whose tweets should be included in the stream")
	public void setFollow(String follow) {
		this.follow = follow;
	}


	public String getLocations() {
		return locations;
	}

	@ModuleOption("comma delimited set of latitude/longitude pairs to include in the stream")
	public void setLocations(String locations) {
		this.locations = locations;
	}

	@NotBlank(message = "You must provide an access token to use this module.")
	public String getAccessToken() {
		return accessToken;
	}

	@ModuleOption("a valid OAuth access token")
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	@NotBlank(message = "You must provide an access token secret to use this module.")
	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	@ModuleOption("an OAuth secret corresponding to the access token")
	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}


	public boolean isDiscardDeletes() {
		return discardDeletes;
	}

	@ModuleOption("set to discard 'delete' events")
	public void setDiscardDeletes(boolean discardDeletes) {
		this.discardDeletes = discardDeletes;
	}

}
