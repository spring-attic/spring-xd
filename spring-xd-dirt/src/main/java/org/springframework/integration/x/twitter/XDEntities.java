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
import java.util.ArrayList;
import java.util.List;

import org.springframework.social.twitter.api.Entities;
import org.springframework.social.twitter.api.HashTagEntity;
import org.springframework.social.twitter.api.MediaEntity;
import org.springframework.social.twitter.api.MentionEntity;
import org.springframework.social.twitter.api.TickerSymbolEntity;
import org.springframework.social.twitter.api.UrlEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDEntities implements Serializable {

	private List<XDUrlEntity> urls;

	private List<XDHashTagEntity> tags;

	private List<XDMentionEntity> mentions;

	private List<XDMediaEntity> media;

	private List<XDTickerSymbolEntity> tickerSymbols;

	public XDEntities() {

	}

	public XDEntities(Entities delegate) {
		if (delegate == null) {
			return;
		}
		if (delegate.getUrls() != null) {
			urls = new ArrayList<XDUrlEntity>();
			for (UrlEntity url : delegate.getUrls()) {
				urls.add(new XDUrlEntity(url));
			}
		}
		if (delegate.getHashTags() != null) {
			tags = new ArrayList<XDHashTagEntity>();
			for (HashTagEntity tag : delegate.getHashTags()) {
				tags.add(new XDHashTagEntity(tag));
			}
		}

		if (delegate.getMentions() != null) {
			mentions = new ArrayList<XDMentionEntity>();
			for (MentionEntity mention : delegate.getMentions()) {
				mentions.add(new XDMentionEntity(mention));
			}
		}

		if (delegate.getMedia() != null) {
			media = new ArrayList<XDMediaEntity>();
			for (MediaEntity medium : delegate.getMedia()) {
				media.add(new XDMediaEntity(medium));
			}
		}

		if (delegate.getTickerSymbols() != null) {
			tickerSymbols = new ArrayList<XDTickerSymbolEntity>();
			for (TickerSymbolEntity tickerSymbol : delegate.getTickerSymbols()) {
				tickerSymbols.add(new XDTickerSymbolEntity(tickerSymbol));
			}
		}
	}


	public List getUrls() {
		return urls;
	}

	public void setUrls(List<XDUrlEntity> urls) {
		this.urls = urls;
	}

	public List getHashTags() {
		return tags;
	}

	public void setHashTags(List<XDHashTagEntity> hashtags) {
		this.tags = hashtags;
	}

	public List getMentions() {
		return mentions;
	}

	public void setMentions(List<XDMentionEntity> mentions) {
		this.mentions = mentions;
	}

	public List getMedia() {
		return media;
	}

	public void setMedia(List<XDMediaEntity> media) {
		this.media = media;
	}


	public List getTickerSymbols() {
		return tickerSymbols;
	}

	public void setTickerSymbols(List<XDTickerSymbolEntity> tickerSymbols) {
		this.tickerSymbols = tickerSymbols;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		XDEntities entities = (XDEntities) o;
		if (media != null ? !media.equals(entities.media) : entities.media != null) {
			return false;
		}
		if (mentions != null ? !mentions.equals(entities.mentions) : entities.mentions != null) {
			return false;
		}
		if (tags != null ? !tags.equals(entities.tags) : entities.tags != null) {
			return false;
		}
		if (urls != null ? !urls.equals(entities.urls) : entities.urls != null) {
			return false;
		}
		if (tickerSymbols != null ? !tickerSymbols.equals(entities.tickerSymbols) : entities.tickerSymbols != null) {
			return false;
		}

		return true;
	}


	@Override
	public int hashCode() {
		int result = urls != null ? urls.hashCode() : 0;
		result = 31 * result + (tags != null ? tags.hashCode() : 0);
		result = 31 * result + (mentions != null ? mentions.hashCode() : 0);
		result = 31 * result + (media != null ? media.hashCode() : 0);
		result = 31 * result + (tickerSymbols != null ? tickerSymbols.hashCode() : 0);
		return result;
	}
}
