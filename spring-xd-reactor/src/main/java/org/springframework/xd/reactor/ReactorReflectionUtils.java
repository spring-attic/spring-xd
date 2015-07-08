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
     * @see {@link #extractGeneric(Object, Class, int)}
     */
    static Class<?> extractGeneric(Object processor, Class<?> target) {
        return extractGeneric(processor, target, 0);
    }

    /**
     * Given the processor interface, determine it's input type parameter
     * @param processor and instance of the processor interface
     * @param target the interface to scan
     * @param index the generic index to retrieve
     * @return the corresponding Class of the processor input type parameter, or null if can't
     * be determined.
     */
    @SuppressWarnings("unchecked")
    static Class<?> extractGeneric(Object processor, Class<?> target, int index) {
        Class<?> searchType = processor.getClass();
        while (searchType != Object.class) {
            if (searchType.getGenericInterfaces().length > 0) {
                for (Type t : searchType.getGenericInterfaces()) {
                    if (ParameterizedType.class.isAssignableFrom(t.getClass())
                        && ((ParameterizedType) t).getRawType().equals(target)) {
                        ParameterizedType pt = (ParameterizedType) t;

                        if (pt.getActualTypeArguments().length == 0) return Message.class;

                        t = pt.getActualTypeArguments()[index];
                        if (t instanceof ParameterizedType) {
                            return (Class<?>) ((ParameterizedType) t).getRawType();
                        } else if (t instanceof Class) {
                            return (Class<?>) t;
                        }
                    }
                }
            }
            searchType = searchType.getSuperclass();
        }
        return Message.class;
    }

    /**
     * Given a processor bean, find the EnableReactorModule annotation for introspection
     */
    @SuppressWarnings("unchecked")
    static EnableReactorModule extractAnnotation(Object processor) {
        Class<?> searchType = processor.getClass();
        while (searchType != Object.class) {
            EnableReactorModule annotation = searchType.getAnnotation(EnableReactorModule.class);
            if(annotation != null){
                return annotation;
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

}
