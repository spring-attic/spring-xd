/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.module.options;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


public class OtherBackingPojo implements ProfileNamesProvider {

	private String fizz = "hello";


	public String getFizz() {
		return fizz;
	}

	@ModuleOption("the fizz option")
	public void setFizz(String fizz) {
		this.fizz = fizz;
	}

	@Override
	public String[] profilesToActivate() {
		if ("bonjour".equals(fizz)) {
			return new String[] { "french-profile" };
		}
		else {
			return NO_PROFILES;
		}
	}


}
