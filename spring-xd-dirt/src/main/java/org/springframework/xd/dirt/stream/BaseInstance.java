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

package org.springframework.xd.dirt.stream;

import java.util.Date;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;

/**
 * A runtime representation of a Definition.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 * @param <D> The definition class representing this instance
 */
public class BaseInstance<D extends BaseDefinition> {

	/**
	 * The {@link D} this running instance embodies.
	 */
	private D definition;

	/**
	 * When was this instance started.
	 */
	private Date startedAt = new Date();

	/**
	 * Status for this deployment unit.
	 */
	private DeploymentUnitStatus status;

	/**
	 * Create a new instance out of the given definition.
	 */
	public BaseInstance(D definition) {
		Assert.notNull(definition, "The definition must not be null.");
		this.definition = definition;
	}

	/**
	 * Return the underlying definition.
	 */
	public D getDefinition() {
		return definition;
	}

	/**
	 * @return The date this instance was started. Will never return null.
	 */
	public Date getStartedAt() {
		return startedAt;
	}

	/**
	 * Set the date when this instance was started.
	 *
	 * @param startedAt Must not be null
	 */
	public void setStartedAt(Date startedAt) {
		Assert.notNull(startedAt, "The passed in date must not be null.");
		this.startedAt = startedAt;
	}

	/**
	 * @return the {@link DeploymentUnitStatus} for this instance.
	 */
	public DeploymentUnitStatus getStatus() {
		return status;
	}

	/**
	 * Set the {@link DeploymentUnitStatus} for this instance.
	 *
	 * @param status the status for this instance
	 */
	public void setStatus(DeploymentUnitStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "BaseInstance [definition=" + definition + ", startedAt=" + startedAt + ", status=" + status + "]";
	}

}
