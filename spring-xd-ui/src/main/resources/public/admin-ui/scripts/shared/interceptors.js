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
 * XD shared (global) interceptors.
 *
 * @author Gunnar Hillert
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdShared.interceptors', [])
      .factory('httpErrorInterceptor', function (XDUtils, $location, $window) {
        return {
          responseError: function (rejection) {
            if (rejection.status === 0) {
              XDUtils.growl.error('Looks like the XD server is down.');
            }
            if (rejection.status === 401) {
              XDUtils.growl.error('Not authenticated. Please login again.');
              $window.location.href = '/admin-ui/login';
            }
            XDUtils.$log.error('Response Error ' + rejection.status, rejection);
            return XDUtils.$q.reject(rejection);
          }
        };
      });
});
