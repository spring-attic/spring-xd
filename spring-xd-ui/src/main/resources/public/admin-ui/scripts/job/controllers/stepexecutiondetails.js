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
 * Definition of the Step Execution Details controller
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', 'StepExecutions', 'XDUtils', '$state', '$stateParams',
    function ($scope, stepExecutions, utils, $state, $stateParams) {

      $scope.showStepExecutionProgress = function (stepExecutionProgress) {
        utils.$log.info('Showing Step Execution Progress');
        utils.$log.info(stepExecutionProgress);
        $state.go('home.jobs.stepexecutionprogress', {
          executionId: $stateParams.executionId,
          stepExecutionId: $stateParams.stepExecutionId
        });
      };
      $scope.refreshStepExecutionProgress = function () {
        utils.$log.info('Refresh Step Execution Progress');
        var stepExecutionProgressPromise = stepExecutions.getStepExecutionProgress($stateParams.executionId, $stateParams.stepExecutionId).$promise;
        utils.addBusyPromise(stepExecutionProgressPromise);
        stepExecutionProgressPromise.then(
            function (result) {
              var percentageFormatted = (result.percentageComplete * 100).toFixed(2);
              result.percentageFormatted = percentageFormatted;
              $scope.stepExecutionProgress = result;
            },
            function (error) {
              utils.growl.error(error);
            });
      };
      $scope.closeStepExecutionDetails = function (stepExecutionDetails) {
        utils.$log.info('Closing Step Execution Details Window');
        $state.go('home.jobs.executiondetails', {executionId: stepExecutionDetails.jobExecutionId});
      };
      $scope.$apply(function () {
        $scope.moduleName = $stateParams.moduleName;
        $scope.optionsPredicate = 'name';

        var singleStepExecutionPromise = stepExecutions.getSingleStepExecution($stateParams.executionId, $stateParams.stepExecutionId).$promise;
        utils.addBusyPromise(singleStepExecutionPromise);

        singleStepExecutionPromise.then(
            function (result) {
              $scope.stepExecutionDetails = result;
            }, function (error) {
              if (error.status === 404) {
                $scope.stepExecutionDetailsNotFound = true;
                $scope.executionId = $stateParams.executionId;
                $scope.stepExecutionId = $stateParams.stepExecutionId;
              }
              else {
                utils.growl.error(error);
              }
            }
        );
        $scope.refreshStepExecutionProgress();
      });
    }];
});