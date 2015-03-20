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
 * Deployment handler that is responsible for deploying/un-deploying the
 * {@link org.springframework.xd.dirt.core.DeploymentUnit} (Stream/Job) to/from the container.
 * The deployment handler assumes the deployment unit (Stream/Job) definitions are persisted in the
 * corresponding repositories.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface DeploymentHandler {

	/**
	 * Deploy the {@link org.springframework.xd.dirt.core.DeploymentUnit} with the given name.
	 *
	 * @param deploymentUnitName the deployment unit name
	 * @throws Exception
	 */
	public void deploy(String deploymentUnitName) throws Exception;

	/**
	 * Un-deploy the {@link org.springframework.xd.dirt.core.DeploymentUnit} with the given name.
	 *
	 * @param deploymentUnitName the deployment unit name
	 * @throws Exception
	 */
	public void undeploy(String deploymentUnitName) throws Exception;
}
