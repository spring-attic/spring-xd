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

import org.springframework.social.twitter.api.HashTagEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDHashTagEntity implements Serializable {

	private String text;

	private int[] indices;

	public XDHashTagEntity() {

	}

	public XDHashTagEntity(HashTagEntity delegate) {
		this.setText(delegate.getText());
		this.setIndices(delegate.getIndices());
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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

		XDHashTagEntity that = (XDHashTagEntity) o;

		if (!Arrays.equals(indices, that.indices)) {
			return false;
		}
		if (text != null ? !text.equals(that.text) : that.text != null) {
			return false;
		}

		return true;
	}


	@Override
	public int hashCode() {
		int result = text != null ? text.hashCode() : 0;
		result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
		return result;
	}
}
