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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.api.java.JavaDStream;

import org.springframework.xd.spark.streaming.SparkConfig;
import org.springframework.xd.spark.streaming.java.Processor;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
@SuppressWarnings({ "serial" })
public class FileLogger implements Processor<JavaDStream<String>, JavaDStream<String>> {

	private File file;

	public void setPath(String filePath) {
		file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			}
			catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	@SparkConfig
	public Properties getSparkConfigProperties() {
		Properties props = new Properties();
		props.setProperty("spark.master", "local[4]");
		return props;
	}

	@Override
	public JavaDStream<String> process(JavaDStream<String> input) {
		input.foreachRDD(new Function<JavaRDD<String>, Void>() {

			@Override
			public Void call(JavaRDD<String> rdd) {
				rdd.foreachPartition(new VoidFunction<Iterator<String>>() {

					@Override
					public void call(Iterator<String> items) throws Exception {
						FileWriter fw;
						BufferedWriter bw = null;
						try {
							fw = new FileWriter(file.getAbsoluteFile());
							bw = new BufferedWriter(fw);
							while (items.hasNext()) {
								bw.append(items.next() + System.lineSeparator());
							}
						}
						catch (IOException ioe) {
							throw new RuntimeException(ioe);
						}
						finally {
							if (bw != null) {
								bw.close();
							}
						}
					}
				});
				return null;
			}
		});
		return null;
	}

}
