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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.xd.test.fixtures.util.FixtureUtils;

/**
 * Support class that represents a tail source.
 *
 * @author Ilayaperumal Gopinathan
 */
public class TailSource extends DisposableFileSupport<TailSource> {

	@Override
	public String toDSL() {
		return String.format("tail --nativeOptions='-f -n +0' --name=%s",
				FixtureUtils.handleShellEscapeProcessing(file.getAbsolutePath()));
	}

	public void appendToFile(String contents) throws IOException {
		FileWriter fileWritter = new FileWriter(file, true);
		BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		bufferWritter.write(contents);
		bufferWritter.close();
	}
}
