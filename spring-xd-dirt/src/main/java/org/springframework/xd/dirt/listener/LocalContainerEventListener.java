/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.listener;

import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;

/**
 * @author David Turanski
 *
 */
public class LocalContainerEventListener extends AbstractContainerEventListener {

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.listener.AbstractContainerEventListener#onContainerStartedEvent(org.springframework.xd.dirt.event.ContainerStartedEvent)
	 */
	@Override
	protected void onContainerStartedEvent(ContainerStartedEvent event) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.listener.AbstractContainerEventListener#onContainerStoppedEvent(org.springframework.xd.dirt.event.ContainerStoppedEvent)
	 */
	@Override
	protected void onContainerStoppedEvent(ContainerStoppedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
