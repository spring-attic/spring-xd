package org.springframework.integration.x.splunk;

import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.splunk.event.SplunkEvent;


/**
 * Message consumer that will push the payload to a splunk instance via a TCP-Port.
 * The Object Payload will be converted to a string and stored in the "data" pair
 *
 * This module uses the Spring-Integration-Splunk Adapter which is built on the
 * splunk.jar
 *
 * @author Glenn Renfro
 */
public class SplunkTransformer {
	private static String EVENT_NAME = "XD-Message";
	private static String EVENT_ID = "XD";
	private static String DATA_KEY = "data";

	@Transformer
		public SplunkEvent generateSplunkEvent(Object s) {
			SplunkEvent data = new SplunkEvent(EVENT_NAME, EVENT_ID);
			data.addPair(DATA_KEY, s.toString());

			return data;
	}
}
