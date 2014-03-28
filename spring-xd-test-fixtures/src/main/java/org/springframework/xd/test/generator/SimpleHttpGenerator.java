/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.test.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.web.client.RestTemplate;


/**
 * Used to generate basic http posts for string and file contents
 * 
 * @author Glenn Renfro
 */
public class SimpleHttpGenerator implements HttpGenerator {

	private transient String host;

	private transient int port;

	private RestTemplate template;

	public SimpleHttpGenerator(String host, int port) throws Exception {
		this.host = host;
		this.port = port;
		template = new RestTemplate();
	}

	@Override
	public void postData(String message) {
		Object[] args = new String[0];
		template.postForObject("http://" + host + ":" + port, message, String.class, args);
	}

	@Override
	public void postFromFile(File file) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Object[] args = new String[0];
		String message = "";
		while (reader.ready()) {
			message += reader.readLine();
		}
		reader.close();
		template.postForObject("http://" + host + ":" + port, message, String.class, args);
	}

}
