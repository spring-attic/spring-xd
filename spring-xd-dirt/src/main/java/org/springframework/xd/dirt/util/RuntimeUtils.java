/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;


/**
 * Utilities for obtaining runtime information for the running process.
 *
 * @author David Turanski
 * @author Patrick Peralta
 */
public abstract class RuntimeUtils {

	/**
	 * Return the name of the host for the machine running this process.
	 *
	 * @return host name for the machine running this process
	 *
	 * @see java.net.InetAddress#getLocalHost
	 * @see java.net.InetAddress#getHostName
	 */
	public static String getHost() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException uhe) {
			host = "unknown";
		}
		return host;
	}

	/**
	 * Return a non loopback IPv4 address for the machine running this process.
	 * If the machine has multiple network interfaces, the IP address for the
	 * first interface returned by {@link java.net.NetworkInterface#getNetworkInterfaces}
	 * is returned.
	 *
	 * @return non loopback IPv4 address for the machine running this process
	 *
	 * @see java.net.NetworkInterface#getNetworkInterfaces
	 * @see java.net.NetworkInterface#getInetAddresses
	 */
	public static String getIpAddress() {
		try {
			for(Enumeration<NetworkInterface> enumNic = NetworkInterface.getNetworkInterfaces();
					enumNic.hasMoreElements();) {
				NetworkInterface ifc = enumNic.nextElement();
				if (ifc.isUp()) {
					for (Enumeration<InetAddress> enumAddr = ifc.getInetAddresses();
							enumAddr.hasMoreElements(); ) {
						InetAddress address = enumAddr.nextElement();
						if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
							return address.getHostAddress();
						}
					}
				}
			}
		}
		catch (IOException e) {
			// ignore
		}

		return "unknown";
	}

	/**
	 * Return the process id for this process.
	 *
	 * @return process id for this process
	 */
	public static int getPid() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.indexOf('@') != -1 ? Integer.parseInt(jvmName.split("@")[0]) : -1;
	}
}
