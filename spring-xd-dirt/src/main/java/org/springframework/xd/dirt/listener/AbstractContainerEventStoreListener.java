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

package org.springframework.xd.dirt.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.xd.dirt.container.AbstractContainerEvent;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.container.store.RuntimeContainerInfoEntity;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractContainerEventStoreListener implements ApplicationListener<AbstractContainerEvent> {

	@Override
	public void onApplicationEvent(AbstractContainerEvent event) {
		if (event instanceof ContainerStartedEvent) {
			final XDContainer container = event.getSource();
			RuntimeContainerInfoEntity entity = new RuntimeContainerInfoEntity(container.getId(), container.getJvmName(),
					container.getHostName(), container.getIpAddress());
			storeContainerEntity(entity);
		}
		else if (event instanceof ContainerStoppedEvent) {
			XDContainer container = event.getSource();
			removeContainerEntity(container.getId());
		}
	}

	protected abstract void storeContainerEntity(RuntimeContainerInfoEntity entity);

	protected abstract void removeContainerEntity(String id);

}
