/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.x.twitter;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Options common to twitter source modules
 *
 * @author David Turanski
 */
public class TwitterMixin {

	private int readTimeout = 9000;

	private int connectTimeout = 5000;

	private String language = "";

	public int getReadTimeout() {
		return readTimeout;
	}

	@ModuleOption("the read timeout for the underlying URLConnection to the twitter stream (ms)")
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	public int getConnectTimeout() {
		return connectTimeout;
	}

	@ModuleOption("the connection timeout for making a connection to Twitter (ms)")
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getLanguage() {
		return language;
	}

	@ModuleOption("language code e.g. 'en'")
	public void setLanguage(String language) {
		this.language = language;
	}

	private String consumerKey;

	private String consumerSecret;


	@NotBlank(message = "You must provide a 'consumerKey' token to use this module.")
	public String getConsumerKey() {
		return consumerKey;
	}


	@ModuleOption("a consumer key issued by twitter")
	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}

	@NotBlank(message = "You must provide a 'consumerSecret' token to use this module.")
	public String getConsumerSecret() {
		return consumerSecret;
	}

	@ModuleOption("consumer secret corresponding to the consumer key")
	public void setConsumerSecret(String consumerSecret) {
		this.consumerSecret = consumerSecret;
	}

}
