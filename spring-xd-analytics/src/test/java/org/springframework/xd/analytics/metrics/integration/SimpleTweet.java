/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.metrics.integration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

public class SimpleTweet {

	private final String from;

	private final String text;

	private final List<String> tags = new ArrayList<String>();

	private final List<String> mentions = new ArrayList<String>();

	private final List<String> tickerSymbols = new ArrayList<String>();


	public SimpleTweet(String from, String tweet) {
		this.from = from;
		this.text = tweet;
		String[] tokens = StringUtils.tokenizeToStringArray(text, " \r\t\n");
		for (String token : tokens) {
			if (token.startsWith("#")) {
				tags.add(token.substring(1).trim());
			}
			else if (token.startsWith("@")) {
				mentions.add(token.substring(1).trim());
			}
			else if (token.startsWith("$")) {
				tickerSymbols.add(token.substring(1).trim());
			}
		}
	}

	public String getFrom() {
		return from;
	}

	public String getText() {
		return text;
	}

	public List<String> getTags() {
		return tags;
	}

	public List<String> getMentions() {
		return mentions;
	}

	public List<String> getTickerSymbols() {
		return tickerSymbols;
	}

	@Override
	public String toString() {
		return "Tweet{" + "from='" + from + '\'' + ", text='" + text + '\''
				+ ", tags=" + tags + ", mentions=" + mentions
				+ ", tickerSymbols=" + tickerSymbols + '}';
	}
}
