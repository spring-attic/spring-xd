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
  return ['$scope', 'XDUtils', '$state', '$stateParams', 'JobDefinitions', 'JobDefinitionService',
    function ($scope, utils, $state, $stateParams, jobDefinitions, jobDefinitionService) {
      $scope.$apply(function () {
        $scope.definitionName = $stateParams.definitionName;
        var singleJobDefinitionPromise = jobDefinitions.getSingleJobDefinition($scope.definitionName);
        utils.$log.info(singleJobDefinitionPromise);
        utils.addBusyPromise(singleJobDefinitionPromise);
        singleJobDefinitionPromise.then(
          function (result) {
            utils.$log.info(result);
            var jobDefinition = result.data;
            if (!jobDefinition) {
              utils.growl.error('No valid job definition returned for definition name ' + $scope.definitionName, jobDefinition);
              return;
            }
            var moduleName = utils.getModuleNameFromJobDefinition(jobDefinition.definition);
            $scope.definitionDeployRequest = {
              jobDefinition: jobDefinition,
              moduleName: moduleName,
              containerMatchCriteria: '',
              jobModuleCount: ''
            };
          },function (error) {
            utils.growl.error('Error fetching job definition. ' + error.data[0].message);
          });
      });
      $scope.cancelDefinitionDeploy = function () {
        utils.$log.info('Cancelling Job Definition Deployment');
        $state.go('home.jobs.tabs.definitions');
      };
      $scope.deployDefinition = function (definitionDeployRequest) {
        utils.$log.info('Deploying Job Definition ' + definitionDeployRequest);
        utils.$log.info('Deploying Job Definition ' + definitionDeployRequest.jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        var properties = [];

        if (definitionDeployRequest.containerMatchCriteria) {
          properties.push('module.' + definitionDeployRequest.moduleName +
          '.criteria=' + definitionDeployRequest.containerMatchCriteria);
        }
        if (definitionDeployRequest.jobModuleCount) {
          properties.push('module.' + definitionDeployRequest.moduleName +
          '.count=' + definitionDeployRequest.jobModuleCount);
        }
        utils.$log.info('Module Deployment Properties:' + properties);

        jobDefinitionService.deploy(definitionDeployRequest.jobDefinition, properties).$promise.then(
            function () {
              utils.growl.success('Deployment Request Sent.');
              $state.go('home.jobs.tabs.definitions');
            },
            function (error) {
              utils.growl.error('Error Deploying Job. ' + error.data[0].message);
            }
          );
      };
    }];
});
