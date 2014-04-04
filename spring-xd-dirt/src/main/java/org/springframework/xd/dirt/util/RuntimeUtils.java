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

package org.springframework.xd.dirt.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 *
 * @author David Turanski
 */
public abstract class RuntimeUtils {

	public final static String getHost() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException uhe) {
			host = "unknown";
		}
		return host;
	}

	public final static String getIpAddress() {
		String ip;

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException uhe) {
			ip = "unknown";
		}
		return ip;

	}

	public final static int getPid() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.indexOf('@') != -1 ? Integer.parseInt(jvmName.split("@")[0]) : -1;
	}
}
