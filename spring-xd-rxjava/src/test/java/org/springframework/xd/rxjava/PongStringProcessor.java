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
package org.springframework.xd.rxjava;

import rx.Observable;
import rx.functions.Func1;

/**
 *
 * A simple stream processor that transforms Strings by adding "-pong" to the string.
 *
 * @author Mark Pollack
 */
public class PongStringProcessor implements Processor<String, String> {

    @Override
    public Observable<String> process(Observable<String> inputStream) {
        return inputStream.map(new Func1<String, String>() {
            @Override
            public String call(String s) {
                return s + "-stringpong";
            }
        });
    }
}
