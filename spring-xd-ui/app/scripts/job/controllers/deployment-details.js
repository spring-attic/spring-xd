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
 * Definition of the Job Deployment Details controller
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', 'XDUtils', '$state', '$stateParams', 'JobDefinitions', 'ModuleMetaData',
    function ($scope, utils, $state, $stateParams, jobDefinitions, moduleMetaData) {
      $scope.$apply(function () {
        $scope.jobName = $stateParams.jobName;

        var singleJobDefinitionPromise = jobDefinitions.getSingleJobDefinition($scope.jobName);
        utils.addBusyPromise(singleJobDefinitionPromise);
        singleJobDefinitionPromise.then(
          function (result) {
            $scope.jobDefinition = result.data;
          }, function (error) {
            if (error.status === 404) {
              $scope.jobDefinitionNotFound = true;
            }
            else {
              utils.growl.error(error);
            }
          }
        );
        var jobModuleMetaDataPromise = moduleMetaData.getModuleMetaDataForJob($scope.jobName).$promise;
        utils.addBusyPromise(jobModuleMetaDataPromise);

        jobModuleMetaDataPromise.then(
          function (result) {
            $scope.jobModuleMetaData = result;
          }, function (error) {
            utils.growl.error(error);
          }
        );
      });
      $scope.closeJobDeploymentDetails = function () {
          utils.$log.info('Closing Job Deployment Details Window');
          $state.go('home.jobs.tabs.deployments');
        };
    }];
});
