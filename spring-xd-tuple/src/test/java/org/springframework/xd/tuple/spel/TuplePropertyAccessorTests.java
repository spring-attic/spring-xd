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

package org.springframework.xd.tuple.spel;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Mark Fisher
 */
public class TuplePropertyAccessorTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final StandardEvaluationContext context = new StandardEvaluationContext();

	@Before
	public void setup() {
		this.context.addPropertyAccessor(new TuplePropertyAccessor());
	}

	@Test
	public void testSimplePropertyByFieldName() {
		Tuple tuple = TupleBuilder.tuple().of("foo", "bar");
		String result = evaluate("foo", tuple, String.class);
		assertEquals("bar", result);
	}

	@Test
	public void testSimplePropertyByIndex() {
		Tuple tuple = TupleBuilder.tuple().of("foo", "bar");
		String result = evaluate("['0']", tuple, String.class);
		assertEquals("bar", result);
	}

	@Test(expected = SpelEvaluationException.class)
	public void failOnNegativeIndex() {
		Tuple tuple = TupleBuilder.tuple().of("foo", "bar");
		evaluate("['-3']", tuple, String.class);
	}

	@Test
	public void testNestedPropertyByFieldName() {
		Tuple child = TupleBuilder.tuple().of("b", 123);
		Tuple tuple = TupleBuilder.tuple().of("a", child);
		int result = evaluate("a.b", tuple, Integer.class);
		assertEquals(123, result);
	}

	@Test
	public void testNestedPropertyByIndex() {
		Tuple child = TupleBuilder.tuple().of("b", 123);
		Tuple tuple = TupleBuilder.tuple().of("a", child);
		int result = evaluate("a['0']", tuple, Integer.class);
		assertEquals(123, result);
	}

	@Test
	public void testNestedPropertyByIndexOnly() {
		Tuple child = TupleBuilder.tuple().of("b", 123);
		Tuple tuple = TupleBuilder.tuple().of("a", child);
		int result = evaluate("['0']['0']", tuple, Integer.class);
		assertEquals(123, result);
	}

	@Test
	public void testArrayPropertyByFieldName() {
		Tuple tuple = TupleBuilder.tuple().of("numbers", new Integer[] { 1, 2, 3 });
		int result = evaluate("numbers[1]", tuple, Integer.class);
		assertEquals(2, result);
	}

	@Test
	public void testArrayPropertyByIndex() {
		Tuple tuple = TupleBuilder.tuple().of("numbers", new Integer[] { 1, 2, 3 });
		int result = evaluate("['0'][0]", tuple, Integer.class);
		assertEquals(1, result);
	}

	@Test
	public void testNestedArrayPropertyByFieldName() {
		Tuple child = TupleBuilder.tuple().of("numbers", new Integer[] { 7, 8, 9 });
		Tuple tuple = TupleBuilder.tuple().of("child", child);
		int result = evaluate("child.numbers[1]", tuple, Integer.class);
		assertEquals(8, result);
	}

	@Test
	public void testNestedArrayPropertyByIndex() {
		Tuple child = TupleBuilder.tuple().of("numbers", new Integer[] { 7, 8, 9 });
		Tuple tuple = TupleBuilder.tuple().of("child", child);
		int result = evaluate("child['0'][2]", tuple, Integer.class);
		assertEquals(9, result);
	}

	@Test
	public void testNestedArrayPropertyByIndexOnly() {
		Tuple child = TupleBuilder.tuple().of("numbers", new Integer[] { 7, 8, 9 });
		Tuple tuple = TupleBuilder.tuple().of("child", child);
		int result = evaluate("['0']['0'][1]", tuple, Integer.class);
		assertEquals(8, result);
	}

	private <T> T evaluate(String expression, Tuple tuple, Class<T> expectedType) {
		return parser.parseExpression(expression).getValue(this.context, tuple, expectedType);
	}

}
