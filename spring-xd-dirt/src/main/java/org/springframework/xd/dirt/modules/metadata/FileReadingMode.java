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

package org.springframework.xd.dirt.modules.metadata;


/**
 * Defines the supported modes of reading and processing files for the
 * {@code File}, {@code FTP} and {@code SFTP} sources. The modes are triggered
 * through Spring Application Context profiles that whose names are accessible via {@link #profile}.
 *
 * @author Gunnar Hillert
 * @since 1.2
 */
public enum FileReadingMode {

	ref("use-ref"),
	lines("use-contents-with-split"),
	contents("use-contents");

	private String profile;

	/**
	 * Constructor.
	 */
	FileReadingMode(final String profile) {
		this.profile = profile;
	}

	/**
	 * @return Spring Application Context profile name that provides the functionality of the respective mode.
	 */
	public String getProfile() {
		return profile;
	}

}
