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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * A tail source for integration tests. Note it does not need to implement {@link DisposableFileSupport}
 * 
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class SimpleTailSource extends AbstractModuleFixture<SimpleTailSource> {

	private static final int DEFAULT_DELAY_IN_MILLIS = 5000;

	private final int delayInMillis;

	private final String fileName;

	/**
	 * Construct a SimpleTailSource with a file delay of 5 seconds
	 */
	public SimpleTailSource() {
		this(DEFAULT_DELAY_IN_MILLIS, SimpleTailSource.class.getName());
	}

	/**
	 * Construct a SimpleTailSource using the provided delay and filename
	 * 
	 * @param delayInMillis millsecond to delay tailing
	 * @param fileName file to tail
	 */
	public SimpleTailSource(int delayInMillis, String fileName) {
		Assert.hasText(fileName, "fileName must not be emptry be null");
		this.delayInMillis = delayInMillis;
		this.fileName = fileName;
	}

	@Override
	protected String toDSL() {
		return String.format("tail --nativeOptions='-F -n +0' --name=%s --fileDelay=%d", fileName, delayInMillis);
	}


}
