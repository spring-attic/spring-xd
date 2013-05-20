package org.springframework.xd.tuple.batch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

public class TupleFieldExtractorTests {

	private TupleFieldExtractor extractor;
	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void setUp() {
		extractor = new TupleFieldExtractor();
	}

	@Test
	@SuppressWarnings("serial")
	public void testExtractWithFields() throws Exception {
		extractor.setNames(new ArrayList<String>() {{
			add("first");
			add("last");
			add("born");
		}});

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		Object[] values = extractor.extract(item);

		assertEquals(3, values.length);
		assertEquals(first, values[0]);
		assertEquals(last, values[1]);
		assertEquals(born, values[2]);
	}

	@Test
	public void testExtractWithoutFields() throws Exception {

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		Object[] values = extractor.extract(item);

		assertEquals(3, values.length);
		assertEquals(first, values[0]);
		assertEquals(last, values[1]);
		assertEquals(born, values[2]);
	}


	@Test
	@Ignore
	@SuppressWarnings("serial")
	public void testExtract_invalidProperty() throws Exception {
		expected.expect(IllegalArgumentException.class);
		expected.expectMessage("Invalid property 'birthday'");

		extractor.setNames(new ArrayList<String>() {{
			add("first");
			add("last");
			add("birthday");
		}});

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		extractor.extract(item);
	}
}
