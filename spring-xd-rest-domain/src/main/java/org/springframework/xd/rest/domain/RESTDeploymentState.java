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

package org.springframework.xd.rest.domain;


/**
 * REST domain representation of org.springframework.xd.dirt.core.DeploymentUnitStatus.State.
 * 
 * @author Ilayaperumal Gopinathan
 */
public enum RESTDeploymentState {

	/**
	 * The deployment unit is not deployed.
	 */
	undeployed,

	/**
	 * One or more of the deployment unit modules are being deployed.
	 */
	deploying,

	/**
	 * All expected modules for the deployment unit have been deployed.
	 */
	deployed,

	/**
	 * Some expected modules for the deployment unit have not been
	 * deployed; however the deployment unit is considered functional.
	 */
	incomplete,

	/**
	 * Some or all of the expected modules for the deployment unit
	 * have failed to deploy; the deployment unit is not considered functional.
	 */
	failed,

	/**
	 * The deployment unit is in the process of being undeployed.
	 */
	undeploying

}
