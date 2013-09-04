
package org.springframework.xd.analytics.metrics.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryFieldValueCounterRepository;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

public class FieldValueCounterHandlerTests {

	private FieldValueCounterRepository repo = new InMemoryFieldValueCounterRepository();

	private final String mentionsFieldValueCounterName = "tweetMentionsCounter";

	@Before
	public void init() {
		repo = new InMemoryFieldValueCounterRepository();
	}

	@Test
	public void messageCountTest() {

		FieldValueCounterHandler handler = new FieldValueCounterHandler(repo, mentionsFieldValueCounterName, "mentions");

		Tuple tuple = TupleBuilder.tuple().of("mentions", Arrays.asList(new String[] { "markp", "markf", "jurgen" }));
		Message<Tuple> message = MessageBuilder.withPayload(tuple).build();

		handler.process(message);
		Map<String, Double> counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("markp"), equalTo(1.0));
		assertThat(counts.get("markf"), equalTo(1.0));
		assertThat(counts.get("jurgen"), equalTo(1.0));

		tuple = TupleBuilder.tuple().of("mentions", Arrays.asList(new String[] { "markp", "jon", "jurgen" }));
		message = MessageBuilder.withPayload(tuple).build();

		handler.process(message);
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("markp"), equalTo(2.0));
		assertThat(counts.get("jon"), equalTo(1.0));
		assertThat(counts.get("jurgen"), equalTo(2.0));

		SimpleTweet tweet = new SimpleTweet("joe", "say hello to @markp and @jon and @jurgen");
		Message<SimpleTweet> msg = MessageBuilder.withPayload(tweet).build();

		handler.process(msg);
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("markp"), equalTo(3.0));
		assertThat(counts.get("jon"), equalTo(2.0));
		assertThat(counts.get("jurgen"), equalTo(3.0));

	}

	@Test
	public void acceptsJson() {
		String json = "{\"mentions\":[\"markp\",\"markf\",\"jurgen\"]}";
		FieldValueCounterHandler handler = new FieldValueCounterHandler(repo, mentionsFieldValueCounterName, "mentions");
		handler.process(new GenericMessage<String>(json));
		Map<String, Double> counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("markp"), equalTo(1.0));
		assertThat(counts.get("markf"), equalTo(1.0));
		assertThat(counts.get("jurgen"), equalTo(1.0));
	}

	@Test
	public void handlesTweetHashtags() throws Exception {
		String tweet = "{\"created_at\":\"Tue Aug 27 18:17:06 +0000 2013\",\"id\":100000,\"text\":\"whocares\",\"retweet_count\":0," +
				"\"entities\":{\"hashtags\":[{\"text\":\"hello\",\"indices\":[23,41]},{\"text\":\"there\",\"indices\":[45,50]}],\"symbols\":[]," +
				"\"urls\":[],\"user_mentions\":[]}," +
				"\"favorited\":false,\"retweeted\":false,\"possibly_sensitive\":false,\"filter_level\":\"medium\",\"lang\":\"bg\"}";

		FieldValueCounterHandler handler = new FieldValueCounterHandler(repo, "hashtags", "entities.hashtags.text");
		handler.process(new GenericMessage<String>(tweet));
		Map<String, Double> counts = repo.findOne("hashtags").getFieldValueCount();
		assertThat(counts.get("hello"), equalTo(1.0));
	}
}
