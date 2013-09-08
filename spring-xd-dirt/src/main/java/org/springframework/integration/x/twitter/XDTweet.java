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

import java.io.Serializable;
import java.util.Date;

import org.springframework.social.twitter.api.Tweet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDTweet implements Serializable {

	private long id;

	private String text;

	private Date createdAt;

	private String fromUser;

	private String profileImageUrl;

	private Long toUserId;

	private long fromUserId;

	private String languageCode;

	private String source;

	private XDTweet retweetedStatus;

	private XDEntities entities;

	private Integer retweetCount;

	private boolean favorited;

	private String inReplyToScreenName;

	private Long inReplyToStatusId;

	private Long inReplyToUserId;

	private boolean retweeted;

	private XDTwitterProfile user;

	public XDTweet() {
	}

	public XDTweet(Tweet delegate) {
		if (delegate == null) {
			return;
		}
		this.setId(delegate.getId());
		this.setText(delegate.getText());
		this.setCreatedAt(delegate.getCreatedAt());
		this.setFromUser(delegate.getFromUser());
		this.setProfileImageUrl(delegate.getProfileImageUrl());
		this.setFromUserId(delegate.getFromUserId());
		this.setToUserId(delegate.getToUserId());
		this.setLanguageCode(delegate.getLanguageCode());
		this.setSource(delegate.getSource());
		this.setXDEntities(new XDEntities(delegate.getEntities()));
		this.setRetweetCount(delegate.getRetweetCount());
		this.setFavorited(delegate.isFavorited());
		this.setInReplyToScreenName(delegate.getInReplyToScreenName());
		this.setInReplyToStatusId(delegate.getInReplyToStatusId());
		this.setRetweetedStatus(new XDTweet(delegate.getRetweetedStatus()));
		this.setInReplyToUserId(delegate.getInReplyToUserId());
		this.setRetweeted(delegate.isRetweet());
		this.setUser(new XDTwitterProfile(delegate.getUser()));
	}


	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public Date getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@JsonProperty("created_at")
	@JsonFormat(pattern = "EEE MMM dd HH:mm:ss Z yyyy")
	public void set_created_at(Date createdAt) {
		this.createdAt = createdAt;
	}


	public String getFromUser() {
		return fromUser;
	}


	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}


	public String getProfileImageUrl() {
		return profileImageUrl;
	}


	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}


	public Long getToUserId() {
		return toUserId;
	}


	public void setToUserId(Long toUserId) {
		this.toUserId = toUserId;
	}


	public long getFromUserId() {
		return fromUserId;
	}


	public void setFromUserId(long fromUserId) {
		this.fromUserId = fromUserId;
	}


	public String getLanguageCode() {
		return languageCode;
	}


	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}


	public String getSource() {
		return source;
	}


	public void setSource(String source) {
		this.source = source;
	}


	public Object getRetweetedStatus() {
		return retweetedStatus;
	}


	public void setRetweetedStatus(XDTweet retweetedStatus) {
		this.retweetedStatus = retweetedStatus;
	}


	public XDEntities getEntities() {
		return entities;
	}


	public void setEntities(XDEntities entities) {
		this.entities = entities;
	}


	public boolean isRetweeted() {
		return this.retweeted;
	}

	public void setRetweeted(boolean retweeted) {
		this.retweeted = retweeted;
	}


	public void setXDEntities(XDEntities entities) {
		this.entities = entities;
	}

	public XDEntities getXDEntities() {
		return this.entities;
	}

	public XDTweet getXDRetweetedStatus() {
		return this.retweetedStatus;
	}


	public Integer getRetweetCount() {
		return retweetCount;
	}


	public void setRetweetCount(Integer retweetCount) {
		this.retweetCount = retweetCount;
	}


	public boolean isFavorited() {
		return favorited;
	}


	public XDTwitterProfile getUser() {
		return user;
	}


	public void setUser(XDTwitterProfile user) {
		this.user = user;
	}

	public void setFavorited(boolean favorited) {
		this.favorited = favorited;
	}


	public Long getInReplyToUserId() {
		return inReplyToUserId;
	}


	public void setInReplyToUserId(Long inReplyToUserId) {
		this.inReplyToUserId = inReplyToUserId;
	}

	public String getInReplyToScreenName() {
		return inReplyToScreenName;
	}


	public void setInReplyToScreenName(String inReplyToScreenName) {
		this.inReplyToScreenName = inReplyToScreenName;
	}


	public Long getInReplyToStatusId() {
		return inReplyToStatusId;
	}


	public void setInReplyToStatusId(Long inReplyToStatusId) {
		this.inReplyToStatusId = inReplyToStatusId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		XDTweet tweet = (XDTweet) o;

		if (fromUserId != tweet.fromUserId) {
			return false;
		}
		if (id != tweet.id) {
			return false;
		}
		if (retweeted != tweet.retweeted) {
			return false;
		}
		if (createdAt != null ? !createdAt.equals(tweet.createdAt) : tweet.createdAt != null) {
			return false;
		}
		if (entities != null ? !entities.equals(tweet.entities) : tweet.entities != null) {
			return false;
		}
		if (fromUser != null ? !fromUser.equals(tweet.fromUser) : tweet.fromUser != null) {
			return false;
		}
		if (inReplyToScreenName != null ? !inReplyToScreenName.equals(tweet.inReplyToScreenName)
				: tweet.inReplyToScreenName != null) {
			return false;
		}
		if (inReplyToStatusId != null ? !inReplyToStatusId.equals(tweet.inReplyToStatusId)
				: tweet.inReplyToStatusId != null) {
			return false;
		}
		if (inReplyToUserId != null ? !inReplyToUserId.equals(tweet.inReplyToUserId) : tweet.inReplyToUserId != null) {
			return false;
		}
		if (languageCode != null ? !languageCode.equals(tweet.languageCode) : tweet.languageCode != null) {
			return false;
		}
		if (profileImageUrl != null ? !profileImageUrl.equals(tweet.profileImageUrl) : tweet.profileImageUrl != null) {
			return false;
		}
		if (retweetCount != null ? !retweetCount.equals(tweet.retweetCount) : tweet.retweetCount != null) {
			return false;
		}
		if (retweetedStatus != null ? !retweetedStatus.equals(tweet.retweetedStatus) : tweet.retweetedStatus != null) {
			return false;
		}
		if (source != null ? !source.equals(tweet.source) : tweet.source != null) {
			return false;
		}
		if (text != null ? !text.equals(tweet.text) : tweet.text != null) {
			return false;
		}
		if (toUserId != null ? !toUserId.equals(tweet.toUserId) : tweet.toUserId != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (text != null ? text.hashCode() : 0);
		result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
		result = 31 * result + (fromUser != null ? fromUser.hashCode() : 0);
		result = 31 * result + (profileImageUrl != null ? profileImageUrl.hashCode() : 0);
		result = 31 * result + (toUserId != null ? toUserId.hashCode() : 0);
		result = 31 * result + (inReplyToStatusId != null ? inReplyToStatusId.hashCode() : 0);
		result = 31 * result + (inReplyToUserId != null ? inReplyToUserId.hashCode() : 0);
		result = 31 * result + (inReplyToScreenName != null ? inReplyToScreenName.hashCode() : 0);
		result = 31 * result + (int) (fromUserId ^ (fromUserId >>> 32));
		result = 31 * result + (languageCode != null ? languageCode.hashCode() : 0);
		result = 31 * result + (source != null ? source.hashCode() : 0);
		result = 31 * result + (retweetCount != null ? retweetCount.hashCode() : 0);
		result = 31 * result + (retweeted ? 1 : 0);
		result = 31 * result + (retweetedStatus != null ? retweetedStatus.hashCode() : 0);
		result = 31 * result + (entities != null ? entities.hashCode() : 0);
		return result;
	}

}
