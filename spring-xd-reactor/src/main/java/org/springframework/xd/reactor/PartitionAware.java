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
package org.springframework.xd.reactor;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import reactor.fn.Supplier;

/**
 * Contract for performing stream processing using on a given expression evaluated partition.
 * <p/>
 * This {@link Supplier} will provide an expression string to be evaluated by an internal {@link SpelExpressionParser}.
 *
 * @author Stephane Maldini
 */
public interface PartitionAware extends Supplier<String>{
}
