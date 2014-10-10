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
  return ['$scope', 'JobDeployments', 'XDUtils', '$state',
    function ($scope, jobDeployments, utils, $state) {
      $scope.pageable = {
        items: {},
        pageNumber: 0,
        pageSize: 5,
        total: 0
      };
      $scope.pagination = {
        current: 1
      };
      $scope.pageChanged = function(newPage) {
        $scope.pageable.pageNumber = newPage-1;
        loadJobDeployments($scope.pageable);
      };
      function loadJobDeployments(pageable) {
        utils.$log.info('pageable', pageable);
        var jobDeploymentsPromise = jobDeployments.getArray().$promise;
        utils.addBusyPromise(jobDeploymentsPromise);
        jobDeploymentsPromise.then(
          function (data) {
            utils.$log.info(data);
            $scope.jobDeployments = data;
          },
          function () {
            utils.growl.addErrorMessage('Error fetching data. Is the XD server running?');
          }
        );
      }
      $scope.viewDeploymentDetails = function (item) {
          utils.$log.info('Showing Deployment details for job: ' + item.name);
          $state.go('home.jobs.deploymentdetails', {jobName: item.name});
      };
      $scope.launchJob = function (item) {
        utils.$log.info('Launching Job: ' + item.name);
        $state.go('home.jobs.tabs.deployments.launch', {jobName: item.name});
      };
      // schedule job
      $scope.scheduleJob = function (item) {
        utils.$log.info('Scheduling Job: ' + item.name);
        $state.go('home.jobs.tabs.deployments.schedule', {jobName: item.name});
      };

      loadJobDeployments($scope.pageable);
    }];
});