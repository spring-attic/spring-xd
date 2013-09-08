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

import org.springframework.social.twitter.api.MentionEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDMentionEntity implements Serializable {

	private long id;

	private String screenName;

	private String name;

	private int[] indices;

	public XDMentionEntity() {

	}

	public XDMentionEntity(MentionEntity delegate) {
		this.setId(delegate.getId());
		this.setScreenName(delegate.getScreenName());
		this.setName(delegate.getName());
		this.setIndices(delegate.getIndices());
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	@JsonProperty("screen_name")
	public void setScreen_Name(String screenName) {
		this.screenName = screenName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

		XDMentionEntity that = (XDMentionEntity) o;

		if (id != that.id) {
			return false;
		}
		if (name != null ? !name.equals(that.name) : that.name != null) {
			return false;
		}
		if (!Arrays.equals(indices, that.indices)) {
			return false;
		}
		if (screenName != null ? !screenName.equals(that.screenName) : that.screenName != null) {
			return false;
		}

		return true;
	}


	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (screenName != null ? screenName.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
		return result;
	}
}
