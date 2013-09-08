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

import org.springframework.social.twitter.api.UrlEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDUrlEntity implements Serializable {

	private String url;

	private String displayUrl;

	private String expandedUrl;

	private int[] indices;

	public XDUrlEntity() {

	}

	public XDUrlEntity(UrlEntity delegate) {
		this.setDisplayUrl(delegate.getDisplayUrl());
		this.setIndices(delegate.getIndices());
		this.setExpandedUrl(delegate.getExpandedUrl());
		this.setUrl(delegate.getUrl());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDisplayUrl() {
		return displayUrl;
	}

	public void setDisplayUrl(String displayUrl) {
		this.displayUrl = displayUrl;
	}

	public String getExpandedUrl() {
		return expandedUrl;
	}

	public void setExpandedUrl(String expandedUrl) {
		this.expandedUrl = expandedUrl;
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

		XDUrlEntity urlEntity = (XDUrlEntity) o;
		if (displayUrl != null ? !displayUrl.equals(urlEntity.displayUrl) : urlEntity.displayUrl != null) {
			return false;
		}
		if (expandedUrl != null ? !expandedUrl.equals(urlEntity.expandedUrl) : urlEntity.expandedUrl != null) {
			return false;
		}
		if (!Arrays.equals(indices, urlEntity.indices)) {
			return false;
		}
		if (url != null ? !url.equals(urlEntity.url) : urlEntity.url != null) {
			return false;
		}
		return true;
	}


	@Override
	public int hashCode() {
		int result = displayUrl != null ? displayUrl.hashCode() : 0;
		result = 31 * result + (expandedUrl != null ? expandedUrl.hashCode() : 0);
		result = 31 * result + (url != null ? url.hashCode() : 0);
		result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
		return result;
	}
}
