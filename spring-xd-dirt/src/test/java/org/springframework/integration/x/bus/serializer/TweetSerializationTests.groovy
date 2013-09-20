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

package org.springframework.integration.x.bus.serializer
import groovy.json.JsonSlurper

import org.junit.Test

import org.springframework.integration.x.bus.serializer.kryo.PojoCodec
import org.springframework.social.twitter.api.Entities
import org.springframework.social.twitter.api.Tweet
import org.springframework.social.twitter.api.TwitterProfile

/**
 *
 * @author David Turanski
 */
class TweetSerializationTests {
	@Test
	void test() {
		def codec = new PojoCodec()
		def jsonSlurper = new JsonSlurper()
		def file = new File("src/test/resources/tweets.out")
		file.each {line->
			def data = jsonSlurper.parseText(line)
			Date createdAt = new Date(data.createdAt)
			Tweet tweet = new Tweet(data.id,data.text,createdAt,data.fromUser,data.profileImageUrl,
					data.toUserId,data.fromUserId,data.languageCode, data.source)
			tweet.entities= new Entities(data.entities.urls,data.entities.hashTags,data.entities.mentions, data.entities.media)
			tweet.user = new TwitterProfile(data.user.id, data.user.screenName, data.user.name, data.user.url, data.user.profileImageUrl, data.user.description, data.user.location, new Date(data.user.createdDate))
			def bos = new ByteArrayOutputStream()
			codec.serialize(tweet, bos)
			def tweet2 = codec.deserialize(bos.toByteArray(), Tweet.class)
			assert tweet == tweet2
		}
	}
}
