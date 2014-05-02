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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the 'TwitterSearch' source module.
 * 
 * @author Glenn Renfro
 */
public class TwitterSearchSource extends AbstractModuleFixture {

	public static final String DEFAULT_OUTPUT_TYPE = "application/json ";

	public static final int DEFAULT_FIXED_DELAY = 30000;

	private String consumerKey;

	private String consumerSecret;

	private int fixedDelay;

	private String query;

	private String outputType;

	/**
	 * Initializes a TwitterSearchSource fixture.
	 * 
	 * @param consumerKey The users twitter consumer key
	 * @param consumerSecret The users twitter comsumer secret key
	 * @param query The query the twitter source will execute.
	 * @param outputType the format in which the output will be written.
	 * @param fixedDelay the number of millis between searches.
	 */
	public TwitterSearchSource(String consumerKey, String consumerSecret, String query, String outputType,
			int fixedDelay) {
		Assert.hasText(consumerKey, "consumerKey must not be empty nor null");
		Assert.hasText(consumerSecret, "consumerSecret must not be empty nor null");
		Assert.hasText(query, "query must not be empty nor null");
		Assert.hasText(outputType, "outputType must not be empty nor null");

		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.query = query;
		this.outputType = outputType;
		this.fixedDelay = fixedDelay;
	}

	/**
	 * Initializes a TwitterSearchSource fixture with a fixed delay of 30000 and an output type of application/json.
	 * 
	 * @param consumerKey The users twitter consumer key
	 * @param consumerSecret The users twitter comsumer secret key
	 * @param query The query the twitter source will execute.
	 * @param fixedDelay
	 */
	public static TwitterSearchSource withDefaults(String consumerKey, String consumerSecret, String query) {
		Assert.hasText(consumerKey, "consumerKey must not be empty nor null");
		Assert.hasText(consumerSecret, "consumerSecret must not be empty nor null");
		Assert.hasText(query, "query must not be empty nor null");

		return new TwitterSearchSource(consumerKey, consumerSecret, query, DEFAULT_OUTPUT_TYPE, DEFAULT_FIXED_DELAY);
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return "twittersearch --fixedDelay=" + fixedDelay + " --consumerKey=" + this.consumerKey
				+ " --consumerSecret=" + consumerSecret + " --query=" + query + " --outputType=application/json ";
	}


	public int getFixedDelay() {
		return fixedDelay;
	}


	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}


}
