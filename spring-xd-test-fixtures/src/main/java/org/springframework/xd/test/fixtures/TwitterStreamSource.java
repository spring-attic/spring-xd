/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the 'TwitterStream' source module.
 *
 * @author Glenn Renfro
 */
public class TwitterStreamSource extends AbstractModuleFixture<TwitterStreamSource> {


	private final String consumerKey;

	private final String consumerSecret;

	private final String accessToken;

	private final String accessTokenSecret;


	/**
	 * Initializes a TwitterStreamSource fixture.
	 *
	 * @param consumerKey the twitter consumer key
	 * @param consumerSecret the twitter consumer secret
	 * @param accessToken the twitter access token
	 * @param accessTokenSecret the twitter token secret
	 */
	public TwitterStreamSource(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
		Assert.hasText(consumerKey, "the consumerKey must not be empty nor null");
		Assert.hasText(consumerSecret, "the consumerSecret must not be empty nor null");
		Assert.hasText(accessToken, "the accessToken must not be empty nor null");
		Assert.hasText(accessTokenSecret, "the accessTokenSecret must not be empty nor null");

		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return "twitterstream --accessToken=" + accessToken + " --consumerKey=" + this.consumerKey
				+ " --consumerSecret=" + consumerSecret + " --accessTokenSecret=" + accessTokenSecret + "  ";
	}


}
