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

package org.springframework.integration.x.twitter;

import org.springframework.social.twitter.api.Tweet;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * 
 * @author David Turanski
 */
public class XDTweet extends org.springframework.social.twitter.api.Tweet {

	/**
	 * @param id
	 * @param text
	 * @param createdAt
	 * @param fromUser
	 * @param profileImageUrl
	 * @param toUserId
	 * @param fromUserId
	 * @param languageCode
	 * @param source
	 */
	public XDTweet() {
		super(0, null, null, null, null, null, 0, null, null);
	}

	public XDTweet(Tweet delegate) {
		super(delegate.getId(), delegate.getText(), delegate.getCreatedAt(),
				delegate.getFromUser(), delegate.getProfileImageUrl(),
				delegate.getFromUserId(), delegate.getToUserId(), delegate.getLanguageCode(),
				delegate.getSource());
	}

	@JsonIgnore
	public void setRetweet(boolean retweet) {

	}
}
