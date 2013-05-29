package org.springframework.xd.dirt.event;

import org.springframework.xd.dirt.core.Container;

/**
 * @author Jennifer Hickey
 */
@SuppressWarnings("serial")
public class ContainerStoppedEvent extends AbstractContainerEvent {

	public ContainerStoppedEvent(Container container) {
		super(container);
	}
}
