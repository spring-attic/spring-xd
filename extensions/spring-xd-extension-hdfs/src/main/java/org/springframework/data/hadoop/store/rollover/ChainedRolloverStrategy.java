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

package org.springframework.data.hadoop.store.rollover;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.hadoop.store.support.OrderedComposite;

/**
 * {@code RolloverStrategy} which is simply chaining other strategies.
 * 
 * @author Janne Valkealahti
 * 
 */
public class ChainedRolloverStrategy implements RolloverStrategy, RolloverSizeAware {

	/** List of ordered composite strategies */
	private OrderedComposite<RolloverStrategy> strategies;

	/**
	 * Instantiates a new chained rollover strategy.
	 */
	public ChainedRolloverStrategy() {
		this(Collections.<RolloverStrategy> emptyList());
	}

	/**
	 * Instantiates a new chained rollover strategy.
	 * 
	 * @param strategies the strategies
	 */
	public ChainedRolloverStrategy(List<? extends RolloverStrategy> strategies) {
		this.strategies = new OrderedComposite<RolloverStrategy>();
		setStrategies(strategies);
	}

	@Override
	public boolean hasRolled() {
		for (Iterator<RolloverStrategy> iterator = strategies.iterator(); iterator.hasNext();) {
			if (iterator.next().hasRolled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void reset() {
		for (Iterator<RolloverStrategy> iterator = strategies.iterator(); iterator.hasNext();) {
			iterator.next().reset();
		}
	}

	@Override
	public void setSize(long size) {
		for (Iterator<RolloverStrategy> iterator = strategies.iterator(); iterator.hasNext();) {
			// TODO: remove casting check for better performance
			RolloverStrategy next = iterator.next();
			if (next instanceof RolloverSizeAware) {
				((RolloverSizeAware) next).setSize(size);
			}
		}
	}

	/**
	 * Sets the list of strategies. This clears all existing strategies.
	 * 
	 * @param strategies the new strategies
	 */
	public void setStrategies(List<? extends RolloverStrategy> strategies) {
		this.strategies.setItems(strategies);
	}

	/**
	 * Register a new strategy.
	 * 
	 * @param strategy the strategy
	 */
	public void register(RolloverStrategy strategy) {
		strategies.add(strategy);
	}

	/**
	 * Gets the strategies.
	 * 
	 * @return the strategies
	 */
	public List<? extends RolloverStrategy> getStrategies() {
		return strategies.getItems();
	}

}
