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
 * Definition of Job Definition controller
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define([], function () {
  'use strict';
  return ['$scope', 'JobDefinitions', 'JobDefinitionService', 'XDUtils',
    function ($scope, jobDefinitions, jobDefinitionService, utils) {

      utils.addBusyPromise(jobDefinitions.get(function (data) {
        utils.$log.info(data);
        $scope.jobDefinitions = data.content;
      }, function (error) {
        utils.$log.error('Error fetching data. Is the XD server running?');
        utils.$log.error(error);
        utils.growl.addErrorMessage('Error fetching data. Is the XD server running?');
      }));

      $scope.deployJob = function (jobDefinition) {
        utils.$log.info('Deploying Job ' + jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        jobDefinitionService.deploy(jobDefinition).$promise.then(
              function () {
                utils.growl.addSuccessMessage('Deployment Request Sent.');
                jobDefinition.deployed = true;
              },
              function (error) {
                utils.$log.error('Error Deploying Job.');
                utils.$log.error(error);
                utils.growl.addErrorMessage('Error Deploying Job.');
              }
            );
      };
      $scope.undeployJob = function (jobDefinition) {
        utils.$log.info('Undeploying Job ' + jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        jobDefinitionService.undeploy(jobDefinition).$promise.then(
              function () {
                utils.growl.addSuccessMessage('Undeployment Request Sent.');
                jobDefinition.deployed = false;
              },
              function (error) {
                utils.$log.error('Error Undeploying Job.');
                utils.$log.error(error);
                utils.growl.addErrorMessage('Error Undeploying Job.');
              }
            );
      };
      $scope.clickModal = function (streamDefinition) {
        $scope.destroyItem = streamDefinition;
      };
      $scope.destroyJob = function (jobDefinition) {
        utils.$log.info('Destroying Job ' + jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        jobDefinitionService.destroy(jobDefinition).$promise.then(
            function () {
              utils.growl.addSuccessMessage('Destroy Request Sent.');
              jobDefinition.inactive = true;
              $scope.closeModal();
            },
            function (error) {
              utils.$log.error('Error Destroying Job.');
              utils.$log.error(error);
              utils.growl.addErrorMessage('Error Destroying Job.');
              $scope.closeModal();
            }
        );
      };
    }];
});
