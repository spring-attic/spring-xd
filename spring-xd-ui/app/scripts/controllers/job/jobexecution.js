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
  return ['$scope', '$http', 'JobExecutions', '$log', 'growl', function ($scope, $http, JobExecutions, $log, growl) {
    JobExecutions.getArray().$promise.then(
        function (result) {
          $log.info('>>>>');
          $log.info(result);
          $scope.jobExecutions = result;
        }, function (error) {
          $log.error('Error fetching data. Is the XD server running?');
          $log.error(error);
          growl.addErrorMessage('Error fetching data. Is the XD server running?');
        });

    $scope.restartJob = function (job) {
      $log.info('Restarting Job ' + job.name);
      JobExecutions.restart(job).$promise.then(
          function (result) {
            $log.info('>>>>');
            $log.info(result);
            $scope.jobExecutions = result;
          }, function (error) {
            $log.error('Error fetching data. Is the XD server running?');
            $log.error(error);
            growl.addErrorMessage('Error fetching data. Is the XD server running?');
            growl.addErrorMessage(error.data[0].message);
          });
    };
  }];
});