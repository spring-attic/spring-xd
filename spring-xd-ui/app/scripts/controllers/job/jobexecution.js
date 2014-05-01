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
define([], function () {
  'use strict';
  return ['$scope', 'JobExecutions', 'XDCommon', '$state', function ($scope, jobExecutions, xdCommon, $state) {
    var list = function () {
      var jobExcutionsPromise = jobExecutions.getArray().$promise;
      xdCommon.addBusyPromise(jobExcutionsPromise);

      jobExcutionsPromise.then(
          function (result) {
            xdCommon.$log.info(result);
            $scope.jobExecutions = result;
          }, function (error) {
            xdCommon.$log.error('Error fetching data. Is the XD server running?');
            xdCommon.$log.error(error);
            xdCommon.growl.addErrorMessage('Error fetching data. Is the XD server running?');
          });
    };
    list();
    $scope.viewJobExecutionDetails = function (jobExecution) {
      xdCommon.$log.info('Showing Job Execution details for Job Execution with Id: ' + jobExecution.executionId);
      $state.go('home.jobs.executiondetails', {executionId: jobExecution.executionId});
    };
    $scope.restartJob = function (job) {
      xdCommon.$log.info('Restarting Job ' + job.name);
      jobExecutions.restart(job).$promise.then(
          function (result) {
            xdCommon.$log.info(result);
            xdCommon.growl.addSuccessMessage('Job was relaunched.');
            list();
          }, function (error) {
            xdCommon.$log.error(error);
            xdCommon.growl.addErrorMessage(error.data[0].message);
          });
    };
  }];
});