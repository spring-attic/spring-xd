/*
 * Copyright 2013 the original author or authors.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Temporary implementation that bypasses Spring Integration to use the TwitterTemplate
 * directly since the Twitter v1.0 API is being deprecated. This can be removed and
 * replaced as soon as we update the Spring Integration Twitter module accordingly.
 *
 * @author Mark Fisher
 * @author Luke Taylor
 */
public class TwitterSearchMessageSource {

	private final String query;

	private final TwitterTemplate template;

	private volatile Long sinceId;

	private final Log logger = LogFactory.getLog(getClass());

	public TwitterSearchMessageSource(String query, OAuth2Template oauthTemplate) {
		Assert.hasText(query, "query is required");
		Assert.notNull(oauthTemplate, "OAuth2Template must not be null");
		this.query = query;
		AccessGrant grant = oauthTemplate.authenticateClient();
		template = new TwitterTemplate(grant.getAccessToken());
	}

	public TwitterSearchMessageSource(String query, TwitterTemplate template) {
		Assert.hasText(query, "query is required");
		Assert.notNull(template, "TwitterTemplate must not be null");
		this.query = query;
		this.template = template;
	}

	public List<Tweet> getTweets() {
		SearchParameters params = new SearchParameters(this.query);
		if (this.sinceId != null) {
			params.sinceId(this.sinceId);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("searching for '" + query + "' since ID: " + this.sinceId);
		}
		SearchResults results = template.searchOperations().search(params);
		if (CollectionUtils.isEmpty(results.getTweets())) {
			return null;
		}
		this.sinceId = results.getTweets().get(0).getId();
		return results.getTweets();
	}

}
