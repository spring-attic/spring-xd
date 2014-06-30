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

import javax.validation.constraints.AssertTrue;

import org.springframework.integration.x.twitter.TwitterSearchChannelAdapter.ResultType;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 *
 * @author David Turanski
 */
@Mixin({ TwitterMixin.class })
public class TwitterSearchOptionsMetadata {

	private String query = "";

	private String geocode = "";

	private ResultType resultType = ResultType.mixed;

	private boolean includeEntities = true;

	public String getQuery() {
		return query;
	}

	@ModuleOption("the query string")
	public void setQuery(String query) {
		this.query = query;
	}

	public String getGeocode() {
		return geocode;
	}

	@ModuleOption("geo-location given as latitude,longitude,radius. e.g., '37.781157,-122.398720,1mi'")
	public void setGeocode(String geocode) {
		this.geocode = geocode;
	}

	@AssertTrue(message = "One of query or geocode (or both) required.")
	public boolean isEitherGeocodeOrQueryRequired() {
		return (StringUtils.hasText(geocode) || StringUtils.hasText(query));
	}

	public ResultType getResultType() {
		return resultType;
	}

	@ModuleOption("result type: recent, popular, or mixed")
	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}


	public boolean isIncludeEntities() {
		return includeEntities;
	}

	@ModuleOption("whether to include entities such as urls, media and hashtags")
	public void setIncludeEntities(boolean includeEntities) {
		this.includeEntities = includeEntities;
	}


}
