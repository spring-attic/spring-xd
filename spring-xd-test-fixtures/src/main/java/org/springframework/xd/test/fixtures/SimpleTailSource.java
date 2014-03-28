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


/**
 * Used by acceptance tests because it does not extend disposable.
 * 
 * @author Glenn Renfro
 */
public class SimpleTailSource extends AbstractModuleFixture {

	private int delay = 5000;

	private String fileName;

	public SimpleTailSource(int delay, String fileName) throws Exception {
		this.delay = delay;
		this.fileName = fileName;
	}

	@Override
	protected String toDSL() {
		return String.format("tail --nativeOptions='-F -n +0' --name=%s --fileDelay=%d", fileName, delay);
	}

	public SimpleTailSource() throws Exception {
		fileName = SimpleTailSource.class.getName();
	}

}
