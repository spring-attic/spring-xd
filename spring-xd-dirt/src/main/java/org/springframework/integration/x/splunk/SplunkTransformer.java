/*
 * Copyright 2011-2013 the original author or authors.
 *
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

package org.springframework.integration.x.splunk;

import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.splunk.event.SplunkEvent;


/**
 * Message consumer that will push the payload to a splunk instance via a TCP-Port. The Object Payload will be converted
 * to a string and stored in the "data" pair
 * 
 * This module uses the Spring-Integration-Splunk Adapter which is built on the splunk.jar
 * 
 * @author Glenn Renfro
 */
public class SplunkTransformer {

	private static String DATA_KEY = "data";

	@Transformer
	public SplunkEvent generateSplunkEvent(Object s) {
		SplunkEvent data = new SplunkEvent();
		data.addPair(DATA_KEY, s.toString());

		return data;
	}
}
