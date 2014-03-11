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
 * Definition of xdAdmin controllers.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdAdmin.controllers', ['xdAdmin.services'])

      .controller('ListDefinitionController',
      function ($scope, $http, JobDefinitions, $log, promiseTracker, $q, $timeout, growl, JobDefinitionService) {

        var testPromise = $q.defer();

        promiseTracker('trackerName').addPromise(testPromise.promise);
        $timeout(function () {
          testPromise.resolve();
        }, 1000);

        JobDefinitions.get(function (data) {
          $log.info(data);
          $scope.jobDefinitions = data.content;
        }, function (error) {
          $log.error('Error fetching data. Is the XD server running?');
          $log.error(error);
          growl.addErrorMessage('Error fetching data. Is the XD server running?');
        });

        $scope.deployJob = function (jobDefinition) {
          $log.info('Deploying Job ' + jobDefinition.name);
          $log.info(JobDefinitionService);
          JobDefinitionService.deploy(jobDefinition);
        };
        $scope.undeployJob = function (jobDefinition) {
          $log.info('Undeploying Job ' + jobDefinition.name);
          $log.info(JobDefinitionService);
          JobDefinitionService.undeploy(jobDefinition);
        };
      })
      .controller('ListJobDeploymentsController', function ($scope, $http, JobDeployments, $log, $state, growl) {
        JobDeployments.getArray(function (data) {
          $log.info(data);
          $scope.jobDeployments = data;

          $scope.launchJob = function (item) {
            $log.info('Launching Job: ' + item.name);
            $state.go('home.jobs.deployments.launch', {jobName: item.name});
          };

        }, function (error) {
          $log.error('Error fetching data. Is the XD server running?');
          $log.error(error);
          growl.addErrorMessage('Error fetching data. Is the XD server running?');
        });
      })
      .controller('ListJobExecutionsController', function ($scope, $http, JobExecutions, $log, growl) {
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
      })
      .controller('LaunchJobController', function ($scope, $http, $log, $state, $stateParams, growl, $location, JobLaunchService) {
        var jobLaunchRequest = $scope.jobLaunchRequest = {
          jobName: $stateParams.jobName,
          jobParameters: []
        };

        $log.info($stateParams);

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
          $log.info('Cancelling Job Launch');
          $state.go('home.jobs.deployments');
        };

        $scope.launchJob = function (jobLaunchRequest) {
          $log.info('Launching Job ' + jobLaunchRequest.jobName);
          JobLaunchService.convertToJsonAndSend(jobLaunchRequest);
          $location.path('/jobs/deployments');
        };
      });
});
