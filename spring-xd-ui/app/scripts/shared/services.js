/*
 * Copyright 2013-2014 the original author or authors.
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
 * XD shared (global) services.
 *
 * @author Ilayaperumal Gopinathan
 */
define(['angular', 'xregexp'], function (angular) {
  'use strict';

  return angular.module('xdShared.services', [])
      .factory('XDUtils', function ($log, growl, $timeout, $q, $rootScope) {

        var moduleNameRegex = new XRegExp('[\\p{N}|\\p{L}|\\p{Po}]*(?=[\\s]*--)', 'i');
        return {
          $log: $log,
          growl: growl,
          $timeout: $timeout,
          $q: $q,
          $rootScope: $rootScope,
          addBusyPromise: function (promise) {
            $rootScope.cgbusy = promise;
          },
          getModuleNameFromJobDefinition: function(jobDefinition) {
            if (!jobDefinition) {
              throw new Error('jobDefinition must be defined.');
            }
            $log.info('Processing job definition: ' + jobDefinition);
            var module = XRegExp.exec(jobDefinition, moduleNameRegex);
            var moduleName;
            if (module) {
              moduleName = module[0];
            }
            else {
              moduleName = jobDefinition;
            }
            $log.info('Found Module Name: ' + moduleName);
            return moduleName;
          }
        };
      })
      .factory('xdVersionInfo', function ($resource, $rootScope, XDUtils) {
        console.log('xdVersionInfo');
        var xdVersionInfoPromise =  $resource($rootScope.xdAdminServerUrl + '/meta/version', {}, {
          query: {
            method: 'GET'
          }
        }).query();
        XDUtils.addBusyPromise(xdVersionInfoPromise);
        return xdVersionInfoPromise;
      });
});
