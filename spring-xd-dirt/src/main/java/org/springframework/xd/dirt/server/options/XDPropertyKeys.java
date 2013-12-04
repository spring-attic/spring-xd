/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.server.options;

/**
 * XD property keys. These should not contain '.', etc. to be compatible with Environment variables
 * 
 * @author David Turanski
 */
public interface XDPropertyKeys {

	public static final String XD_HOME = "XD_HOME";

	public static final String XD_TRANSPORT = "XD_TRANSPORT";

	public static final String XD_CONTROL_TRANSPORT = "XD_CONTROL_TRANSPORT";

	public static final String XD_JMX_ENABLED = "XD_JMX_ENABLED";

	public static final String XD_ANALYTICS = "XD_ANALYTICS";

	public static final String XD_HADOOP_DISTRO = "XD_HADOOP_DISTRO";

	public static final String XD_STORE = "XD_STORE";

	public static final String XD_JMX_PORT = "XD_JMX_PORT";

	public static final String XD_HTTP_PORT = "XD_HTTP_PORT";

}
