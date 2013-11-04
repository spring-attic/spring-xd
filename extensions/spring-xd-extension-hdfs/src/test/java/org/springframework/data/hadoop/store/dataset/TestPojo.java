package org.springframework.data.hadoop.store.dataset;

import java.io.Serializable;
import java.util.Date;

/**
 */
public class TestPojo implements Comparable<TestPojo> {
	Long id;
	String name;
	Date birthDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + id + " " + name + " " + birthDate;
	}

	@Override
	public int compareTo(TestPojo aTestPojo) {
		if (id.longValue() < aTestPojo.getId().longValue()) {
			return -1;
		}
		if (id.longValue() > aTestPojo.getId().longValue()) {
			return +1;
		}
		return 0;
	}
}
