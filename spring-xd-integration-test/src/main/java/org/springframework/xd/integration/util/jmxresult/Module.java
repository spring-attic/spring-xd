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

import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an XD Module that is returned from a JMX Query.
 *
 * @author Glenn Renfro
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Module {

	@JsonProperty("SendCount")
	private String sendCount;

	@JsonProperty("TimeSinceLastSend")
	private String timeSinceLastSend;

	@JsonProperty("MeanSendRate")
	private String meanSendRate;

	@JsonProperty("MeanSendDuration")
	private String meanSendDuration;

	@JsonProperty("SendErrorCount")
	private String sendErrorCount;

	@JsonProperty("StandardDeviationSendDuration")
	private String standardDeviationSendDuration;

	@JsonProperty("MaxSendDuration")
	private String maxSendDuration;

	@JsonProperty("MeanErrorRatio")
	private String meanErrorRatio;

	@JsonProperty("MeanErrorRate")
	private String meanErrorRate;

	@JsonProperty("MinSendDuration")
	private String minSendDuration;

	private String request;

	private String moduleName;

	private String moduleChannel;

	private String receiveCount;

	private String receiveErrorCount;

	/**
	 * Retrieves the module data from the key and value data returned from Jackson.
	 *
	 * @param key The key for the module
	 * @param map The value associated for the module.
	 * @return Fully qualified Module Instance.
	 */
	public static Module generateModuleFromJackson(String key, Map<?, ?> map) {
		Module module = new Module();
		BeanWrapper bw = new BeanWrapperImpl(module);
		MutablePropertyValues mpv = new MutablePropertyValues(map);
		mpv = addKeyValuesToProperties(key, mpv);
		bw.setPropertyValues(mpv, true, true);
		return module;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getModuleChannel() {
		return moduleChannel;
	}

	public void setModuleChannel(String moduleChannel) {
		this.moduleChannel = moduleChannel;
	}

	public String getSendCount() {
		return sendCount;
	}

	public void setSendCount(String sendCount) {
		this.sendCount = sendCount;
	}

	public String getTimeSinceLastSend() {
		return timeSinceLastSend;
	}

	public void setTimeSinceLastSend(String timeSinceLastSend) {
		this.timeSinceLastSend = timeSinceLastSend;
	}

	public String getMeanSendRate() {
		return meanSendRate;
	}

	public void setMeanSendRate(String meanSendRate) {
		this.meanSendRate = meanSendRate;
	}

	public String getMeanSendDuration() {
		return meanSendDuration;
	}

	public void setMeanSendDuration(String meanSendDuration) {
		this.meanSendDuration = meanSendDuration;
	}

	public String getSendErrorCount() {
		return sendErrorCount;
	}

	public void setSendErrorCount(String sendErrorCount) {
		this.sendErrorCount = sendErrorCount;
	}

	public String getStandardDeviationSendDuration() {
		return standardDeviationSendDuration;
	}

	public void setStandardDeviationSendDuration(
			String standardDeviationSendDuration) {
		this.standardDeviationSendDuration = standardDeviationSendDuration;
	}

	public String getMaxSendDuration() {
		return maxSendDuration;
	}

	public void setMaxSendDuration(String maxSendDuration) {
		this.maxSendDuration = maxSendDuration;
	}

	public String getMeanErrorRatio() {
		return meanErrorRatio;
	}

	public void setMeanErrorRatio(String meanErrorRatio) {
		this.meanErrorRatio = meanErrorRatio;
	}

	public String getMeanErrorRate() {
		return meanErrorRate;
	}

	public void setMeanErrorRate(String meanErrorRate) {
		this.meanErrorRate = meanErrorRate;
	}

	public String getMinSendDuration() {
		return minSendDuration;
	}

	public void setMinSendDuration(String minSendDuration) {
		this.minSendDuration = minSendDuration;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getReceiveCount() {
		return receiveCount;
	}


	public void setReceiveCount(String receiveCount) {
		this.receiveCount = receiveCount;
	}


	public String getReceiveErrorCount() {
		return receiveErrorCount;
	}


	public void setReceiveErrorCount(String receiveErrorCount) {
		this.receiveErrorCount = receiveErrorCount;
	}

	/**
	 * The key has the module name and module channel in a comma delimited list. This method extracts those values and
	 * places them in new PropertyValues.
	 *
	 * @param key The key that contains the module name and channel .
	 * @param properties The MutablePropertyValues that the module name and module channel will be added to.
	 * @return A new MutablePropertyValues that has the contents of the properties passed in and the contents of the
	 *         key.
	 */
	private static MutablePropertyValues addKeyValuesToProperties(String key, MutablePropertyValues properties) {
		MutablePropertyValues result = new MutablePropertyValues();
		result.addPropertyValues(properties);
		String moduleComponents[] = StringUtils
				.commaDelimitedListToStringArray(key);
		for (String keyComponent : moduleComponents) {
			String[] tokens = StringUtils.tokenizeToStringArray(
					keyComponent, "=");
			if (tokens.length == 2) {
				if (tokens[0].equals("module")) {
					result.add("moduleName", tokens[1]);
				}
				if (tokens[0].equals("name")) {
					result.add("moduleChannel", tokens[1]);
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "Module [sendCount=" + sendCount + ", timeSinceLastSend=" + timeSinceLastSend + ", meanSendRate="
				+ meanSendRate + ", meanSendDuration=" + meanSendDuration + ", sendErrorCount=" + sendErrorCount
				+ ", standardDeviationSendDuration=" + standardDeviationSendDuration + ", maxSendDuration="
				+ maxSendDuration + ", meanErrorRatio=" + meanErrorRatio + ", meanErrorRate=" + meanErrorRate
				+ ", minSendDuration=" + minSendDuration + ", request=" + request + ", moduleName=" + moduleName
				+ ", moduleChannel=" + moduleChannel + ", receiveCount=" + receiveCount + ", receiveErrorCount="
				+ receiveErrorCount + "]";
	}

}
