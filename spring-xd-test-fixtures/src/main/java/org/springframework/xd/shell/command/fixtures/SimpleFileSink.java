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

package org.springframework.xd.shell.command.fixtures;



/**
 * Used to generate the file sink portion of a stream.  
 * Used by Acceptance testing because it does not extend disposable.
 * 
 * @author Glenn Renfro
 */
public class SimpleFileSink extends AbstractModuleFixture {

	private String dir;

	private String fileName;

	@Override
	protected String toDSL() {
		return String.format("file --binary=true --mode=%s", "REPLACE");
	}

	public String toDSL(String mode, String binary) {
		String result = String.format("file  --mode=%s --binary=%s ", mode, binary);
		if (fileName != null) {
			result += "--name='" + fileName + "' ";
		}
		if (dir != null) {
			result += "--dir='" + dir + "'";
		}
		return result;
	}

	public SimpleFileSink() {
		dir = null;
		fileName = null;
	}

	public SimpleFileSink(String dir, String fileName) {
		this.dir = dir;
		this.fileName = fileName;
	}

}
