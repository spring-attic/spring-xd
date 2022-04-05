/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.tuple.batch;

/**
 * Enum used to provide typing information when reading from a file.
 * 
 * @author Michael Minella
 * @see TupleFieldSetMapper
 */
public enum FieldSetType {
	STRING, CHAR, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BIG_DECIMAL, DATE;
}
