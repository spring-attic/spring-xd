/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.module.util;

import java.util.Date;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;


/**
 * 
 * @author Glenn Renfro
 */
public class DateTrigger implements Trigger {

	private volatile Date nextFireDate;

	private volatile boolean singleFire = false;

	public DateTrigger() {
		singleFire = true;
		nextFireDate = new Date();
	}

	public DateTrigger(Date nextFireDate) {
		this.nextFireDate = nextFireDate;
	}

	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date result = null;
		if (singleFire) {
			singleFire = false;
			return nextFireDate;
		}
		return result;
	}

}
