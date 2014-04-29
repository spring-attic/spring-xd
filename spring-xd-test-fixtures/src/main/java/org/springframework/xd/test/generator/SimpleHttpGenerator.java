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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.google.common.io.Files;


/**
 * Used to generate basic http posts for string and file contents
 * 
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class SimpleHttpGenerator implements HttpGenerator {

	private final String url;

	private final RestTemplate restTemplate;

	public SimpleHttpGenerator(String host, int port) {
		Assert.hasText(host, "host must not be null or empty");
		url = "http://" + host + ":" + port;
		restTemplate = new RestTemplate();
	}

	@Override
	public void postData(String message) {
		restTemplate.postForObject(url, message, String.class);
	}

	@Override
	public void postFromFile(File file) {
		Assert.notNull(file, "file must not be null");
		try {
			restTemplate.postForObject(url, Files.toString(file, Charset.defaultCharset()), String.class);
		}
		catch (IOException e) {
			throw new GeneratorException("Could not read from file [" + file + "]", e);
		}
	}


}
