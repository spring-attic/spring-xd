/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.util.jmxresult;

/**
 * The Module/Channel that is returned when a JMX Query is exected against XD.
 *
 * @author Glenn Renfro
 */
public class JMXChannelResult {

	private String timestamp;

	private String status;

	private JMXRequest request;

	private Module value;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public JMXRequest getRequest() {
		return request;
	}

	public void setRequest(JMXRequest request) {
		this.request = request;
	}

	public Module getValue() {
		return value;
	}

	public void setValue(Module value) {
		this.value = value;
	}

}
