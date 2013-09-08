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

import org.springframework.social.twitter.api.TickerSymbolEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author David Turanski
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XDTickerSymbolEntity implements Serializable {

	private String tickerSymbol;

	private String url;

	private int[] indices;

	public XDTickerSymbolEntity() {

	}

	public XDTickerSymbolEntity(TickerSymbolEntity delegate) {
		this.setTickerSymbol(delegate.getTickerSymbol());
		this.setUrl(delegate.getUrl());
		this.setIndices(delegate.getIndices());
	}


	public String getTickerSymbol() {
		return tickerSymbol;
	}


	public void setTickerSymbol(String tickerSymbol) {
		this.tickerSymbol = tickerSymbol;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
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

		XDTickerSymbolEntity that = (XDTickerSymbolEntity) o;

		if (tickerSymbol != null ? !tickerSymbol.equals(that.tickerSymbol) : that.tickerSymbol != null) {
			return false;
		}

		if (url != null ? !url.equals(that.url) : that.url != null) {
			return false;
		}

		return true;
	}


	@Override
	public int hashCode() {
		int result = tickerSymbol.hashCode() ^ (tickerSymbol.hashCode());
		result = 31 * result + (url != null ? url.hashCode() : 0);
		result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
		return result;
	}
}
