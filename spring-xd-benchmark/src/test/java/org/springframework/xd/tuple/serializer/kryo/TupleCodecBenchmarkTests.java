/*
 * Copyright 2014 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.tuple.serializer.kryo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.util.StopWatch;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author David Turanski
 * @author Gary Russell
 */
public class TupleCodecBenchmarkTests {
	private static final int ITERATIONS = 50000;

	private static final int NUM_FIELDS = 10;

	private PojoCodec serializer;

	private PojoCodec deserializer;

	private Random random = new Random(System.currentTimeMillis());


	private Tuple[] generateSamples(boolean includeNestedObjects) {
		Tuple[] tuples = new Tuple[ITERATIONS];
		for (int i = 0; i < ITERATIONS; i++) {
			tuples[i] = randomTuple(NUM_FIELDS, includeNestedObjects);
		}
		return tuples;
	}

	@Before
	public void setUp() {
		serializer = new PojoCodec(new TupleKryoRegistrar());
		deserializer = new PojoCodec(new TupleKryoRegistrar());
	}

	@Test
	public void runBenchmarks() throws IOException {
		StopWatch stopWatch = new StopWatch("Tuple ser/deser - Iterations:" + ITERATIONS + " number of fields:" +
				NUM_FIELDS);

		Tuple[] primitiveTuples = generateSamples(false);
		Tuple[] nestedTuples = generateSamples(true);

		//warm up
		long endTime = System.currentTimeMillis() + 5000;
		int i = 0;
		do {
			runBenchmark(stopWatch, "warmup  " + i, primitiveTuples, serializer, deserializer);
			i++;
		} while (System.currentTimeMillis() < endTime);


		runBenchmark(stopWatch, "primitives", primitiveTuples, serializer, deserializer);
		runBenchmark(stopWatch, "nested", nestedTuples, serializer, deserializer);
		System.out.println(stopWatch.prettyPrint());
		for (StopWatch.TaskInfo taskInfo : stopWatch.getTaskInfo()) {
			if (taskInfo.getTaskName().equals("primitives") || taskInfo.getTaskName().equals("nested")) {
				double nanosecs = taskInfo.getTimeMillis() * 1000000.0;
				double averagens = nanosecs / ITERATIONS;
				System.out.println(taskInfo.getTaskName() + ": avg time (ns) " + averagens);
			}
		}

	}

	private void runBenchmark(StopWatch stopWatch, String taskName, Tuple[] tuples, PojoCodec serializer,
			PojoCodec deserializer) throws IOException {
		stopWatch.start(taskName);
		for (int i = 0; i < ITERATIONS; i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			serializer.encode(tuples[i], bos);
			byte[] bytes = bos.toByteArray();
			Tuple result = deserializer.decode(bytes, DefaultTuple.class);
			assertEquals(tuples[i].getFieldNames(), result.getFieldNames());
			assertEquals(tuples[i].getValues(), result.getValues());
			assertNotNull(((DefaultTuple) tuples[i]).getConversionService());
		}
		stopWatch.stop();
	}


	//TODO: primitive arrays
	//TODO: Other types supported by Tuple, e.g. BigInteger
	//TODO: Can this be simplified with autoboxing?
	private Tuple randomTuple(int numFields, boolean includeNestedObjects) {
		Class<?>[] types = new Class<?>[] {int.class, boolean.class, char.class, long.class, float.class,
				double.class, byte.class, Byte.class, Integer.class, Boolean.class, Long.class, Double.class,
				String.class, Foo.class, Tuple.class};

		int range = includeNestedObjects ? types.length : types.length - 2;

		TupleBuilder tupleBuilder = new TupleBuilder();
		for (int i = 0; i < numFields; i++) {

			String name = "field" + i;
			Class<?> type = types[random.nextInt(range)];

			Object value = null;
			if (type.equals(int.class)) {
				value = random.nextInt();
			}
			else if (type.equals(Integer.class)) {
				value = new Integer(random.nextInt());
			}
			else if (type.equals(boolean.class)) {
				value = random.nextBoolean();
			}
			else if (type.equals(Boolean.class)) {
				value = new Boolean(random.nextBoolean());
			}
			else if (type.equals(char.class)) {
				value = 'q';
			}
			else if (type.equals(long.class)) {
				value = Math.abs(random.nextLong());
			}
			else if (type.equals(Long.class)) {
				value = Math.abs(new Long(random.nextLong()));
			}
			else if (type.equals(byte.class)) {
				value = (byte) random.nextInt(127);
			}
			else if (type.equals(Byte.class)) {
				value = new Byte((byte) random.nextInt(127));
			}

			else if (type.equals(float.class)) {
				value = random.nextFloat();
			}
			else if (type.equals(Float.class)) {
				value = new Float(random.nextFloat());
			}

			else if (type.equals(double.class)) {
				value = random.nextDouble();
			}
			else if (type.equals(Double.class)) {
				value = new Double(random.nextDouble());
			}

			else if (type.equals(String.class)) {
				value = new BigInteger(130, random).toString(32);
			}

			else if (type.equals(Foo.class)) {
				value = new Foo();
			}
			else if (type.equals(Tuple.class)) {
				value = randomTuple(5, false);
			}
			tupleBuilder.put(name, value);
		}
		return tupleBuilder.build();
	}

	static class Foo {
		private String s = "";

		private boolean b;

		private int i;

		private long l;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Foo foo = (Foo) o;

			if (b != foo.b) return false;
			if (i != foo.i) return false;
			if (l != foo.l) return false;
			if (!s.equals(foo.s)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = s.hashCode();
			result = 31 * result + (b ? 1 : 0);
			result = 31 * result + i;
			result = 31 * result + (int) (l ^ (l >>> 32));
			return result;
		}


		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}

		public boolean isB() {
			return b;
		}

		public void setB(boolean b) {
			this.b = b;
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

		public long getL() {
			return l;
		}

		public void setL(long l) {
			this.l = l;
		}
	}

}
