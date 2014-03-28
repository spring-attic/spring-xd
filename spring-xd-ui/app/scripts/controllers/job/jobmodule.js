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
 * Handles user logins.
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', 'JobModuleService', 'growl', '$log', '$state',
          function ($scope, JobModuleService, growl, $log, $state) {
          $scope.jobModules = {};

          JobModuleService.getAllModules().$promise.then(
             function (result) {
                 $log.info('>>>>');
                 $log.info(result);
                 $scope.jobModules = result.content;
               }, function (error) {
                 $log.error('Error fetching data. Is the XD server running?');
                 $log.error(error);
                 growl.addErrorMessage('Error fetching data. Is the XD server running?');
               }
             );
          $scope.viewModuleDetails = function (item) {
              $log.info('Showing Module details for module: ' + item.name);
              $state.go('home.jobs.modulesdetails', {moduleName: item.name});
            };
    }];
});
