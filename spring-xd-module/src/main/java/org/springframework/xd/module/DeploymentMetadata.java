package org.springframework.xd.module;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Represents information about a particular module deployment.
 * Extensible using {@link #addAttribute(String, Object)}.
 * @author Luke Taylor
 * @author Gary Russell
 */
public class DeploymentMetadata {

	private final String group;

	private final int    index;

	private final String sourceChannelName;

	private final String sinkChannelName;

	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public DeploymentMetadata(String group, int index) {
		this(group, index, null, null);
	}

	public DeploymentMetadata(String group, int index, String sourceChannelName, String sinkChannelName) {
		Assert.notNull(group);
		this.group = group;
		this.index = index;
		this.sourceChannelName = sourceChannelName;
		this.sinkChannelName = sinkChannelName;
	}

	public String getGroup() {
		return group;
	}

	public int getIndex() {
		return index;
	}

	public String getInputChannelName() {
		return sourceChannelName == null ? group + "." + (index - 1) : sourceChannelName;
	}

	public String getOutputChannelName() {
		return sinkChannelName == null ? group + "." + index : sinkChannelName;
	}

	public synchronized void addAttribute(String key, Object value) {
		this.attributes.put(key, value);
	}

	public synchronized Object removeAttribute(String key) {
		return this.attributes.remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name, Class<T> type) {
		Object value = this.attributes.get(name);
		if (value != null) {
			Assert.isInstanceOf(type, value);
			return (T) value;
		}
		return null;
	}

}
