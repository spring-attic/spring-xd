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


/**
 * Generate a single http request from a string or file payload.
 *
 * Implementation would be expect to set the URL to post to in their constructor
 *
 * @author Mark Pollack
 */
public interface HttpGenerator {

	/**
	 * Generate a http POST request using the String as the body of the request.
	 *
	 * @param message String to send in the http body.
	 */
	void postData(String message);

	/**
	 * Generate a http request from the contents of a file
	 *
	 * @param file the File that contains the data to post
	 * @throws GeneratorException If there was an error related to file handling.
	 */
	void postFromFile(File file);

}
