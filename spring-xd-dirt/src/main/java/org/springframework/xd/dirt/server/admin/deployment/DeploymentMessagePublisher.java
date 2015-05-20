/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.server.admin.deployment;

/**
 * Publisher that defines the contract to publish the {@link DeploymentMessage}.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface DeploymentMessagePublisher {

	/**
	 * Publish the {@link DeploymentMessage} and immediately
	 * return.
	 *
	 * @param message the deployment message
	 */
	void publish(DeploymentMessage message);

	/**
	 * Publish the {@link DeploymentMessage} and block the
	 * executing thread until the message has been processed
	 * by the recipient.
	 *
	 * @param message the deployment message
	 */
	void poll(DeploymentMessage message);
}
