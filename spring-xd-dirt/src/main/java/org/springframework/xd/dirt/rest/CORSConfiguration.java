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
package org.springframework.xd.dirt.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Configuration class to ensure CORS headers are added to every web response 
 * 
 * @author Andrew Eisenberg
 */
@Configuration
public class CORSConfiguration {
    
    @Autowired
    @Bean(name="headerMappingInterceptor")
    public MappedInterceptor headerMappingInterceptor() {
        return new MappedInterceptor(new String[] { "/**/*", "/*" }, new CORSHeaderInterceptor());
    }
    
    
    static class CORSHeaderInterceptor extends HandlerInterceptorAdapter {

        // TODO Access-Control-Allow-Origin header value is hard coded. Should be configurable
        private static final String ALLOWED_ORIGIN = "http://localhost:9889";

        @Override
        public boolean preHandle(HttpServletRequest request,
                HttpServletResponse response, Object handler) throws Exception {
            if (!response.containsHeader("Access-Control-Allow-Origin")) {
                response.addHeader("Access-Control-Allow-Origin",
                        ALLOWED_ORIGIN);
            }
            if (!response.containsHeader("Access-Control-Allow-Methods")) {
                response.addHeader("Access-Control-Allow-Methods",
                        "GET, POST, DELETE, OPTIONS");
            }
            return true;
        }
    }
}


