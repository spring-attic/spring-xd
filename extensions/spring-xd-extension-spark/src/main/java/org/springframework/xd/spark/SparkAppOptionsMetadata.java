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

package org.springframework.xd.spark;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Module options for Spark application module.
 *
 * @author Ilayaperumal Gopinathan
 */
public class SparkAppOptionsMetadata {

	private String name = "";

	private String master = "local";

	private String mainClass;

	private String appJar;

	private String programArgs = "";

	private String conf = "";

	private String files = "";

	public String getName() {
		return name;
	}

	@ModuleOption("the name of the Spark application")
	public void setName(String name) {
		this.name = name;
	}

	public String getMaster() {
		return master;
	}

	@ModuleOption("the master URL for Spark")
	public void setMaster(String master) {
		this.master = master;
	}

	public String getMainClass() {
		return mainClass;
	}

	@ModuleOption("the main class for Spark application")
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getAppJar() {
		return appJar;
	}

	@ModuleOption("path to a bundled jar that includes your application and its dependencies - excluding spark")
	public void setAppJar(String appJar) {
		this.appJar = appJar;
	}

	public String getProgramArgs() {
		return programArgs;
	}

	@ModuleOption("program arguments for the application main class")
	public void setProgramArgs(String programArgs) {
		this.programArgs = programArgs;
	}

	public String getConf() {
		return conf;
	}

	@ModuleOption("comma seperated list of key value pairs as config properties")
	public void setConf(String conf) {
		this.conf = conf;
	}

	public String getFiles() {
		return files;
	}

	@ModuleOption("comma separated list of files to be placed in the working directory of each executor")
	public void setFiles(String files) {
		this.files = files;
	}

}
