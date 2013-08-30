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

package org.springframework.xd.shell.command.fixtures;


/**
 * Fixture that represents the mail source module, which uses polling (here forced to 1s).
 * 
 * @author Eric Bottard
 */
public class MailSource extends AbstractMailSource {

	@Override
	protected String toDSL() {
		return String.format("mail --port=%d --protocol=%s --folder=%s --username=%s --password=%s --fixedDelay=1",
				port, protocol,
				folder, ADMIN_USER, ADMIN_PASSWORD);
	}

}
