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

package org.springframework.xd.dirt.server.options;

/**
 * The Hadoop distribution to use.
 * 
 * @author Thomas Risberg
 * 
 */
public enum HadoopDistro {

	/**
	 * Apache Hadoop 1.0
	 */
	hadoop10,

    /**
     * Apache Hadoop 1.1
     */
    hadoop11,

    /**
     * Apache Hadoop 2.0
     */
    hadoop20,

    /**
	 * Pivotal HD 1.0
	 */
	phd1;

}
