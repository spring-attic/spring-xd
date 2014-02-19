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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;

import uk.co.it.modular.hamcrest.date.DateMatchers;

/**
 * A sanity check to test the built in shell's Date command
 * 
 * @author Mark Pollack
 * 
 */
public class DateCommandTests extends AbstractShellIntegrationTest {

	@Test
	public void testDate() throws ParseException {

		CommandResult cr = getShell().executeCommand("date");

		assertTrue("Date command completed correctly", cr.isSuccess());
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
		Date result = df.parse(cr.getResult().toString());
		Date now = new Date();
		MatcherAssert.assertThat(now, DateMatchers.within(5, TimeUnit.SECONDS, result));
	}

}
