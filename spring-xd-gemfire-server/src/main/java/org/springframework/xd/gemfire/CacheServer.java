/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.gemfire;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * @author David Turanski
 *
 */
public class CacheServer {
	
	private static final Log logger = LogFactory.getLog(CacheServer.class);
	
	public static void main(String[] args) {
 		if (args.length != 1) {
 			System.out.println("Usage: CacheServer <config-file-path>");
 			System.exit(1);
 		}
 		String path = args[0];
		logger.info("Starting Cache Server");
 		@SuppressWarnings("resource")
 		FileSystemXmlApplicationContext context= new FileSystemXmlApplicationContext(path);
 		context.registerShutdownHook();
	}
}
