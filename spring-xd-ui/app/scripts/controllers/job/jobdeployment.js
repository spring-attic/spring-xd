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
 * Definition of Job Deployment controller
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define([], function () {
  'use strict';
  return ['$scope', '$http', 'JobDeployments', '$log', '$state', 'growl',
    function ($scope, $http, JobDeployments, $log, $state, growl) {
      JobDeployments.getArray(function (data) {
        $log.info(data);
        $scope.jobDeployments = data;

        $scope.launchJob = function (item) {
          $log.info('Launching Job: ' + item.name);
          $state.go('home.jobs.tabs.deployments.launch', {jobName: item.name});
        };

      }, function (error) {
        $log.error('Error fetching data. Is the XD server running?');
        $log.error(error);
        growl.addErrorMessage('Error fetching data. Is the XD server running?');
      });
    }];
});