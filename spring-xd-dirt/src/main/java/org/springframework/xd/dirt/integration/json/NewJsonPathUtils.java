/*
 *
 *  * Copyright 2011-2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.integration.json;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

/**
 * Similar to {@code org.springframework.integration.json.JsonPathUtils}, but compatible with newer versions of
 * json-path that expect a Predicate vararg.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 */
public final class NewJsonPathUtils {

	private NewJsonPathUtils() {
	}

	public static <T> T evaluate(Object json, String jsonPath, Predicate... filters) throws Exception {
		if (json instanceof String) {
			return JsonPath.read((String) json, jsonPath, filters);
		}
		else if (json instanceof File) {
			return JsonPath.read((File) json, jsonPath, filters);
		}
		else if (json instanceof URL) {
			return JsonPath.read((URL) json, jsonPath, filters);
		}
		else if (json instanceof InputStream) {
			return JsonPath.read((InputStream) json, jsonPath, filters);
		}
		else {
			return JsonPath.read(json, jsonPath, filters);
		}

	}
}
