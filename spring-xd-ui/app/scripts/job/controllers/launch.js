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
 * Definition of Job Launch controller
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define([], function () {
  'use strict';
  return ['$scope', 'JobLaunchService', 'XDUtils', '$state', '$stateParams', '$location',
    function ($scope, jobLaunchService, utils, $state, $stateParams, $location) {
      $scope.$apply(function () {
        var jobLaunchRequest = $scope.jobLaunchRequest = {
          jobName: $stateParams.jobName,
          jobParameters: []
        };

        utils.$log.info($stateParams);

        $scope.addParameter = function () {
          jobLaunchRequest.jobParameters.push({key: '', value: '', type: 'string'});
        };

        $scope.removeParameter = function (jobParameter) {
          for (var i = 0, ii = jobLaunchRequest.jobParameters.length; i < ii; i++) {
            if (jobParameter === jobLaunchRequest.jobParameters[i]) {
              $scope.jobLaunchRequest.jobParameters.splice(i, 1);
            }
          }
        };

        $scope.dataTypes = [
          {id: 1, key: 'string', name: 'String', selected: true},
          {id: 2, key: 'date', name: 'Date'},
          {id: 3, key: 'long', name: 'Long'},
          {id: 4, key: 'double', name: 'Double'}
        ];

        $scope.cancelJobLaunch = function () {
          utils.$log.info('Cancelling Job Launch');
          $state.go('home.jobs.tabs.deployments');
        };

        $scope.launchJob = function (jobLaunchRequest) {
          utils.$log.info('Launching Job ' + jobLaunchRequest.jobName);
          jobLaunchService.convertToJsonAndSend(jobLaunchRequest);
          $location.path('/jobs/deployments');
        };
      });
    }];
});
