/*
 *
 *  * Copyright 2014-2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import java.util.ArrayList;
import java.util.List;

/**
 * SparkPi Sample Code that utilizes spark master calls to generate a rough estimate of pi.
 *
 * @author Glenn Renfro
 */
@SuppressWarnings("serial")
public class SparkPi {

	public static void main(String[] args) throws Exception {
		SparkConf sparkConf = new SparkConf().setAppName("TestSparkPi");
		JavaSparkContext jsc = new JavaSparkContext(sparkConf);

		int slices = (args.length == 1) ? Integer.parseInt(args[0]) : 2;
		int n = 100000 * slices;
		List<Integer> l = new ArrayList<Integer>(n);
		for (int i = 0; i < n; i++) {
			l.add(i);
		}

		JavaRDD<Integer> dataSet = jsc.parallelize(l, slices);

		int count = dataSet.map(new Function<Integer, Integer>() {
			@Override
			public Integer call(Integer integer) {
				double x = Math.random() * 2 - 1;
				double y = Math.random() * 2 - 1;
				return (x * x + y * y < 1) ? 1 : 0;
			}
		}).reduce(new Function2<Integer, Integer, Integer>() {
			@Override
			public Integer call(Integer integer, Integer integer2) {
				return integer + integer2;
			}
		});

		System.out.println("Pi is roughly " + 4.0 * count / n);

		System.out.println("Complete");
	}

}
