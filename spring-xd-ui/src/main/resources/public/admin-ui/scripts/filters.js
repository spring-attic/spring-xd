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

/**
 * Definition of custom filters.
 *
 * @author Gunnar Hillert
 */
define(['angular'], function(angular) {
  'use strict';
  angular.module('xdAdmin.filters', [])
    .filter('capitalize', function() {
      return function(input) {
        if (input) {
          return input.substring(0,1).toUpperCase()+input.substring(1);
        }
        return input;
      };
    })
    .filter('camelCaseToHuman', function() {
      return function(input) {
        if (input) {
          return input.charAt(0).toUpperCase() + input.substr(1).replace(/[A-Z]/g, ' $&');
        }
        return input;
      };
    });
});
