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
 * Definition of the Module Details controller
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', '$http', '$log', '$state', '$stateParams', 'growl', '$location', 'JobModuleService',
    function ($scope, $http, $log, $state, $stateParams, growl, $location, JobModuleService) {
      $scope.$apply(function () {
        $scope.moduleName = $stateParams.moduleName;
        $scope.optionsPredicate = 'name';
        JobModuleService.getSingleModule($stateParams.moduleName).$promise.then(
            function (result) {
                $scope.moduleDetails = result;
              }, function (error) {
                $log.error('Error fetching data. Is the XD server running?');
                $log.error(error);
                growl.addErrorMessage('Error fetching data. Is the XD server running?');
              }
            );

        JobModuleService.getModuleDefinition($stateParams.moduleName).success(function(data){
          $scope.moduleDefinition = data;
        })
        .error(function(error){
          $log.error('Error fetching data. Is the XD server running?');
          $log.error(error);
          growl.addErrorMessage('Error fetching data. Is the XD server running?');
        });
        $scope.closeModuleDetails = function () {
            $log.info('Closing Job Details Window');
            $state.go('home.jobs.tabs.modules');
          };
      });
    }];
});
