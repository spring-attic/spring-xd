
package org.springframework.xd.analytics.metrics.integration;

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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class FieldValueCounterHandlerTests {

	private FieldValueCounterRepository repo = new InMemoryFieldValueCounterRepository();

	private final String mentionsFieldValueCounterName = "tweetMentionsCounter";

	@Before
	@After
	public void initAndCleanup() {
		repo.delete(mentionsFieldValueCounterName);
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

}
