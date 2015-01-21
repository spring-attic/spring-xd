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
 * Definition of Job Execution controller
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define(['model/pageable'], function (Pageable) {
  'use strict';
  return ['$scope', 'JobExecutions', 'XDUtils', '$state', function ($scope, jobExecutions, utils, $state) {
    $scope.pageable = new Pageable();
    $scope.pagination = {
      current: 1
    };
    $scope.pageChanged = function(newPage) {
      $scope.pageable.pageNumber = newPage-1;
      loadJobExecutions($scope.pageable);
    };

    function loadJobExecutions(pageable) {
      var jobExcutionsPromise = jobExecutions.getAllJobExecutions(pageable).$promise;
      utils.addBusyPromise(jobExcutionsPromise);

      jobExcutionsPromise.then(
          function (result) {
            utils.$log.info('job excutions', result);
            $scope.pageable.items = result.content;
            $scope.pageable.total = result.page.totalElements;
            utils.$log.info('$scope.pageable', $scope.pageable);
          }, function () {
            utils.growl.error('Error fetching data. Is the XD server running?');
          });
    }
    $scope.viewJobExecutionDetails = function (jobExecution) {
      utils.$log.info('Showing Job Execution details for Job Execution with Id: ' + jobExecution.executionId);
      $state.go('home.jobs.executiondetails', {executionId: jobExecution.executionId});
    };
    $scope.restartJob = function (job) {
      utils.$log.info('Restarting Job ' + job.name);
      jobExecutions.restart(job).$promise.then(
          function (result) {
            utils.$log.info(result);
            utils.growl.success('Job was relaunched.');
            loadJobExecutions($scope.pageable);
          }, function (error) {
            if (error.data[0].logref === 'NoSuchBatchJobException') {
              utils.growl.error('The BatchJob ' + job.name + ' is currently not deployed.');
            }
            else {
              utils.growl.error(error.data[0].message);
            }
          });
    };
    $scope.stopJob = function (job) {
      utils.$log.info('Stopping Job ' + job.name);
      jobExecutions.stop(job).$promise.then(
          function (result) {
            utils.$log.info(result);
            utils.growl.success('Stop request sent.');
            loadJobExecutions($scope.pageable);
          }, function (error) {
            utils.growl.error(error.data[0].message);
          });
    };
    loadJobExecutions($scope.pageable);
  }];
});