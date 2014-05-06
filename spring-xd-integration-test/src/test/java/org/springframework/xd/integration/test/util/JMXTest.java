/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import org.springframework.xd.integration.util.jmxresult.JMXResult;
import org.springframework.xd.integration.util.jmxresult.Module;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Verifies that the JMX results returned from XD can be parsed.
 *
 * @author Glenn Renfro
 */
public class JMXTest {


	private static final String JSON_RESULT = "{\"timestamp\":1399329377,\"status\":200,\"request\":"
			+ "{\"mbean\":\"xd.foo:component=MessageChannel,module=*,name=*\",\"type\":\"read\"}"
			+ ",\"value\":{\"xd.foo:component=MessageChannel,module=time.0,name=output\":"
			+ "{\"SendCount\":3,\"TimeSinceLastSend\":0.966,\"MeanSendRate\":2.1315812592430388,"
			+ "\"MeanSendDuration\":1.6309963099630995,\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.4825349383993369,"
			+ "\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0},"
			+ "\"xd.foo:component=MessageChannel,module=log.1,name=input\":"
			+ "{\"SendCount\":3,\"TimeSinceLastSend\":0.967,\"MeanSendRate\":1.173077899672431,\"MeanSendDuration\":1.2988929889298892,"
			+ "\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.45777283678528413,\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,"
			+ "\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0}}}";

	private static final String JSON_MISSING_NAME_CHANNEL_RESULT = "{\"timestamp\":1399329377,\"status\":200,\"request\":"
			+ "{\"mbean\":\"xd.foo:component=MessageChannel,module=*,name=*\",\"type\":\"read\"}"
			+ ",\"value\":{\"xd.foo:component=MessageChannel,module=time.0\":"
			+ "{\"SendCount\":3,\"TimeSinceLastSend\":0.966,\"MeanSendRate\":2.1315812592430388,"
			+ "\"MeanSendDuration\":1.6309963099630995,\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.4825349383993369,"
			+ "\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0},"
			+ "\"xd.foo:component=MessageChannel \":"
			+ "{\"TimeSinceLastSend\":0.967,\"MeanSendRate\":1.173077899672431,\"MeanSendDuration\":1.2988929889298892,"
			+ "\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.45777283678528413,\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,"
			+ "\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0}}}";


	private static final String JSON_EXTRA_DATA_RESULT = "{\"timestamp\":1399329377,\"status\":200,\"request\":"
			+ "{\"mbean\":\"xd.foo:component=MessageChannel,module=*,name=*\",\"type\":\"read\"}"
			+ ",\"value\":{\"xd.foo:component=MessageChannel,module=time.0,ogg=foo,name=output\":"
			+ "{\"SendCount\":3,\"ReceiveCount\":5,\"FakeCount\":3,\"TimeSinceLastSend\":0.966,\"MeanSendRate\":2.1315812592430388,"
			+ "\"MeanSendDuration\":1.6309963099630995,\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.4825349383993369,"
			+ "\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0},"
			+ "\"xd.foo:component=MessageChannel,module=log.1,name=input\":"
			+ "{\"SendCount\":3,\"ResultCount\":5,\"TimeSinceLastSend\":0.967,\"MeanSendRate\":1.173077899672431,\"MeanSendDuration\":1.2988929889298892,"
			+ "\"SendErrorCount\":0,\"StandardDeviationSendDuration\":0.45777283678528413,\"MaxSendDuration\":2.0,\"MeanErrorRatio\":0.0,"
			+ "\"MeanErrorRate\":0.0,\"MinSendDuration\":1.0}}}";

	/**
	 * Verifies that a normal Json result can be parsed.
	 */
	@Test
	public void testFullyQualifiedResult() {
		List<Module> modules = parseJsonToModule(JSON_RESULT);
		assertEquals("Expected 2 modules in this result.", 2, modules.size());
		Module module = modules.get(0);
		assertEquals("Expected moduleName is time.0", "time.0", module.getModuleName());
		assertEquals("Expected moduleChannel is output", "output", module.getModuleChannel());
		assertEquals("Should be only 3 sends", "3", module.getSendCount());

		module = modules.get(1);
		assertEquals("Expected moduleName is log.1", "log.1", module.getModuleName());
		assertEquals("Expected moduleChannel is input", "input", module.getModuleChannel());
		assertEquals("Should be only 3 sends", "3", module.getSendCount());
	}

	/**
	 * Verifies that a Missing Channel, Module name and sendCount.
	 */
	@Test
	public void testFullyMissingData() {
		List<Module> modules = parseJsonToModule(JSON_MISSING_NAME_CHANNEL_RESULT);
		assertEquals("Expected 2 modules in this result.", 2, modules.size());
		Module module = modules.get(0);
		assertEquals("Expected moduleName is time.0", "time.0", module.getModuleName());
		assertNull("Expected moduleChannel is null", module.getModuleChannel());
		assertEquals("Should be only 3 sends", "3", module.getSendCount());
		module = modules.get(1);
		assertNull("Expected moduleName to be null ", module.getModuleName());
		assertNull("Expected moduleChannel to be null", module.getModuleChannel());
		assertNull("Should be only 3 sends", module.getSendCount());
	}

	/**
	 * Verifies that extra data will not prevent a parse.
	 */
	@Test
	public void testExtraDataInJson() {
		List<Module> modules = parseJsonToModule(JSON_EXTRA_DATA_RESULT);
		assertEquals("Expected 2 modules in this result.", 2, modules.size());
		Module module = modules.get(0);
		assertEquals("Expected moduleName is time.0", "time.0", module.getModuleName());
		assertEquals("Expected moduleChannel is output", "output", module.getModuleChannel());
		assertEquals("Should be only 3 sends", "3", module.getSendCount());
		assertEquals("Should be only 5 receive", "5", module.getReceiveCount());

		module = modules.get(1);
		assertEquals("Expected moduleName is log.1", "log.1", module.getModuleName());
		assertEquals("Expected moduleChannel is input", "input", module.getModuleChannel());
		assertEquals("Should be only 3 sends", "3", module.getSendCount());
		assertNull("receiveCount should be null", module.getReceiveCount());

	}


	private List<Module> parseJsonToModule(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JMXResult jmxResult = mapper.readValue(json,
					new TypeReference<JMXResult>() {
			});
			return jmxResult.getValue().getModules();
		}
		catch (JsonParseException parseException) {
			throw new IllegalStateException(parseException.getMessage(), parseException);
		}
		catch (JsonMappingException mapException) {
			throw new IllegalStateException(mapException.getMessage(), mapException);
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}

	}
}
