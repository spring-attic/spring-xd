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

/**
 * @author Glenn Renfro
 */
package org.springframework.xd.integration.util.jmxresult;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.util.StringUtils;

public class Module {
	private String sendCount;
	private String timeSinceLastSend;
	private String meanSendRate;
	private String meanSendDuration;
	private String sendErrorCount;
	private String standardDeviationSendDuration;
	private String maxSendDuration;
	private String meanErrorRatio;
	private String meanErrorRate;
	private String minSendDuration;
	private String request;
	private String moduleName;
	private String moduleChannel;

	private final static int MODULE_NAME_OFFSET = 1;
	private final static int MODULE_CHANNEL_OFFSET = 2;

	public static Module generateModuleFromJackson(String key, Object value)
			throws Exception {
		@SuppressWarnings("unchecked")
		Module module = setupModule((LinkedHashMap<String, Object>) value);
		String moduleComponents[] = StringUtils
				.commaDelimitedListToStringArray(key);
		module.setModuleName(StringUtils.tokenizeToStringArray(
				moduleComponents[MODULE_NAME_OFFSET], "=")[1]);
		module.setModuleChannel(StringUtils.tokenizeToStringArray(
				moduleComponents[MODULE_CHANNEL_OFFSET], "=")[1]);

		return module;
	}

	static private Module setupModule(LinkedHashMap<String, Object> value)
			throws Exception {
		Module module = new Module();
		Iterator<Entry<String, Object>> iter = value.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> entry = iter.next();
			Method setter = module.getClass().getMethod("set" + entry.getKey(),
					String.class);
			setter.invoke(module, entry.getValue().toString());
		}
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

	@Override
	public String toString() {
		return "Module [sendCount=" + sendCount + ", timeSinceLastSend="
				+ timeSinceLastSend + ", meanSendRate=" + meanSendRate
				+ ", meanSendDuration=" + meanSendDuration
				+ ", sendErrorCount=" + sendErrorCount
				+ ", standardDeviationSendDuration="
				+ standardDeviationSendDuration + ", maxSendDuration="
				+ maxSendDuration + ", meanErrorRatio=" + meanErrorRatio
				+ ", meanErrorRate=" + meanErrorRate + ", minSendDuration="
				+ minSendDuration + ", request=" + request + ", moduleName="
				+ moduleName + ", moduleChannel=" + moduleChannel + "]";
	}

}
