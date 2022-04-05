/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.server.admin.deployment;

import org.springframework.xd.dirt.DirtException;

/**
 * Exception thrown when {@link DeploymentHandler} encounters problem
 * when deploying/un-deploying stream/job.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class DeploymentException extends DirtException {

	public DeploymentException(String message){
		super(message);
	}

	public DeploymentException(String message, Exception e){
		super(message, e);
	}

}
