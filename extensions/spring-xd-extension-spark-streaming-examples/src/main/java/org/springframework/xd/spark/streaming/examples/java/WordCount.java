/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.spark.streaming.examples.java;

import java.util.Arrays;
import java.util.Properties;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import org.springframework.xd.spark.streaming.SparkConfig;
import org.springframework.xd.spark.streaming.java.Processor;

import scala.Tuple2;

/**
 * @author Mark Fisher
 * @since 1.1
 */
@SuppressWarnings({ "serial" })
public class WordCount implements Processor<JavaDStream<String>, JavaPairDStream<String, Integer>> {

	@Override
	public JavaPairDStream<String, Integer> process(JavaDStream<String> input) {
		JavaDStream<String> words = input.flatMap(new FlatMapFunction<String, String>() {

			@Override
			public Iterable<String> call(String x) {
				return Arrays.asList(x.split(" "));
			}
		});
		JavaPairDStream<String, Integer> wordCounts = words.mapToPair(new PairFunction<String, String, Integer>() {

			@Override
			public Tuple2<String, Integer> call(String s) {
				return new Tuple2<String, Integer>(s, 1);
			}
		}).reduceByKey(new Function2<Integer, Integer, Integer>() {

			@Override
			public Integer call(Integer i1, Integer i2) {
				return i1 + i2;
			}
		});
		return wordCounts;
	}

	@SparkConfig
	public Properties getSparkConfigProperties() {
		Properties props = new Properties();
		props.setProperty(SPARK_MASTER_URL_PROP, "local[4]");
		return props;
	}
}
