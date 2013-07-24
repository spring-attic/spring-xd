package org.springframework.xd.shell.command;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.shell.AbstractShellIntegrationTest;

import uk.co.it.modular.hamcrest.date.DateMatchers;


public class DateCommandTest extends AbstractShellIntegrationTest {

	@Test
	public void testDate() throws ParseException {
		Bootstrap bootstrap = new Bootstrap();
		
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		
		CommandResult cr = shell.executeCommand("date");
		
		assertTrue("Date command completed correctly", cr.isSuccess());
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,Locale.US);
		Date result = df.parse(cr.getResult().toString());
		Date now = new Date();
		MatcherAssert.assertThat(now, DateMatchers.within(5, TimeUnit.SECONDS, result));	
	}

}
