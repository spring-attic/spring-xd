package org.springframework.xd.test;

import org.apache.avro.reflect.Nullable;

/**
 * Test POJO for writing partitioned dataset
 */
public class TestPojo {

	Integer id;

	Long timestamp;

	@Nullable
	String description;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
