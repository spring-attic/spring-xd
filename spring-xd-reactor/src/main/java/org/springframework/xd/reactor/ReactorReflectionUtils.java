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

import org.springframework.messaging.Message;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility class for working with the reflection API for Reactor based processors.
 *
 * <p>Only intended for internal use.
 *
 * @author Stephane Maldini
 * @author Mark Pollack
 *
 */
public abstract class ReactorReflectionUtils {

    /**
     * Given the processor interface, determine it's input type parameter
     * @param processor and instance of the processor interface
     * @return the corresponding Class of the processor input type parameter, or null if can't
     * be determined.
     */
    public static Class<?> extractGeneric(Processor<?, ?> processor) {
        Class<?> searchType = processor.getClass();
        while (searchType != Object.class) {
            if (searchType.getClass().getGenericInterfaces().length == 0) {
                continue;
            }
            for (Type t : searchType.getGenericInterfaces()) {
                if (ParameterizedType.class.isAssignableFrom(t.getClass())) {
                    ParameterizedType pt = (ParameterizedType) t;
                    if (pt.getActualTypeArguments().length == 0) {
                        return Message.class;
                    }
                    t = pt.getActualTypeArguments()[0];
                    if (t instanceof ParameterizedType) {
                        return (Class) ((ParameterizedType) t).getRawType();
                    } else if (t instanceof Class) {
                        return (Class) t;
                    }
                }
            }
            searchType = searchType.getSuperclass();
        }
        return Message.class;

    }

}
