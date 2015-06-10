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
package org.springframework.xd.reactor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility class for working with the reflection API for Reactor based processors.
 * <p/>
 * <p>Only intended for internal use.
 *
 * @author Stephane Maldini
 * @author Mark Pollack
 */
public abstract class ReactorReflectionUtils {

    /**
     * Given the processor interface, determine it's input type parameter
     *
     * @param processor and instance of the processor interface
     * @return the corresponding Class of the processor input type parameter, or null if can't
     * be determined.
     */
    public static Class<?> extractInputType(Processor<?, ?> processor) {
        Class<?> searchType = processor.getClass();
        while (searchType != Object.class) {
            for (Type t : searchType.getGenericInterfaces()) {
                // if it is a parameterized
                if (t instanceof ParameterizedType && Processor.class.equals(((ParameterizedType) t).getRawType())) {
                    Type inputTypeArgument = ((ParameterizedType) t).getActualTypeArguments()[0];
                    if (inputTypeArgument instanceof ParameterizedType) {
                        return (Class) ((ParameterizedType) inputTypeArgument)
                                .getRawType();
                    } else if (inputTypeArgument instanceof Class) {
                        return (Class) inputTypeArgument;
                    }
                } else if (t instanceof Class && Processor.class.equals(t)) {
                    return Object.class;
                }
            }
            searchType = searchType.getSuperclass();
        }
        throw new IllegalStateException("Cannot identify the input type");
    }

}
