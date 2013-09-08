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
import java.util.Arrays;

import org.springframework.social.twitter.api.MediaEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDMediaEntity implements Serializable {

	private long id;

	private String mediaHttp;

	private String mediaHttps;

	private String url;

	private String display;

	private String expanded;

	private String type;

	private int[] indices;

	public XDMediaEntity() {

	}

	public XDMediaEntity(MediaEntity delegate) {
		this.setId(delegate.getId());
		this.setMediaUrl(delegate.getMediaUrl());
		this.setMediaSecureUrl(delegate.getMediaSecureUrl());
		this.setUrl(delegate.getUrl());
		this.setDisplayUrl(delegate.getDisplayUrl());
		this.setExpandedUrl(delegate.getExpandedUrl());
		this.setType(delegate.getType());
		this.setIndices(delegate.getIndices());
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getMediaUrl() {
		return mediaHttp;
	}

	public void setMediaUrl(String mediaHttp) {
		this.mediaHttp = mediaHttp;
	}

	public String getMediaSecureUrl() {
		return mediaHttps;
	}

	public void setMediaSecureUrl(String mediaHttps) {
		this.mediaHttps = mediaHttps;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDisplayUrl() {
		return display;
	}

	public void setDisplayUrl(String display) {
		this.display = display;
	}

	public String getExpandedUrl() {
		return expanded;
	}

	public void setExpandedUrl(String expanded) {
		this.expanded = expanded;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int[] getIndices() {
		return indices;
	}

	public void setIndices(int[] indices) {
		this.indices = indices;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		XDMediaEntity that = (XDMediaEntity) o;

		if (id != that.id) {
			return false;
		}
		if (display != null ? !display.equals(that.display) : that.display != null) {
			return false;
		}
		if (expanded != null ? !expanded.equals(that.expanded) : that.expanded != null) {
			return false;
		}
		if (!Arrays.equals(indices, that.indices)) {
			return false;
		}
		if (mediaHttp != null ? !mediaHttp.equals(that.mediaHttp) : that.mediaHttp != null) {
			return false;
		}
		if (mediaHttps != null ? !mediaHttps.equals(that.mediaHttps) : that.mediaHttps != null) {
			return false;
		}
		if (type != null ? !type.equals(that.type) : that.type != null) {
			return false;
		}
		if (url != null ? !url.equals(that.url) : that.url != null) {
			return false;
		}

		return true;
	}


	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (mediaHttp != null ? mediaHttp.hashCode() : 0);
		result = 31 * result + (mediaHttps != null ? mediaHttps.hashCode() : 0);
		result = 31 * result + (url != null ? url.hashCode() : 0);
		result = 31 * result + (display != null ? display.hashCode() : 0);
		result = 31 * result + (expanded != null ? expanded.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
		return result;
	}
}
