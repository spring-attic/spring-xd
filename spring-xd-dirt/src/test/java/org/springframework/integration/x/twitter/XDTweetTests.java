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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.social.twitter.api.Tweet;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * 
 * @author David Turanski
 */
public class XDTweetTests {

	@Test
	public void test() {
		Tweet tweet = new Tweet(0L, "hello", new Date(), "from", null, new Long(0), 0L, "en", "source");
		XDTweet xdtweet = new XDTweet(tweet);

	}

	@Test
	public void testJsonToObject() throws IOException {
		Resource jsonFile = new ClassPathResource("/org/springframework/integration/x/twitter/tweet.json");
		assertTrue(jsonFile.exists());

		BufferedReader reader = new BufferedReader(new InputStreamReader(jsonFile.getInputStream()));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		reader.close();
		String json = stringBuilder.toString();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(json.getBytes("UTF-8"));
		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		XDTweet tweet = mapper.treeToValue(root, XDTweet.class);

	}
}
