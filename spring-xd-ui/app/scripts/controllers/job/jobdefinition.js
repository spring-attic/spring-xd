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
  return ['$scope', 'JobDefinitions', 'JobDefinitionService', 'XDCommon',
    function ($scope, jobDefinitions, jobDefinitionService, xdCommon) {

      jobDefinitions.get(function (data) {
        xdCommon.$log.info(data);
        $scope.jobDefinitions = data.content;
      }, function (error) {
        xdCommon.$log.error('Error fetching data. Is the XD server running?');
        xdCommon.$log.error(error);
        xdCommon.growl.addErrorMessage('Error fetching data. Is the XD server running?');
      });

      $scope.deployJob = function (jobDefinition) {
        xdCommon.$log.info('Deploying Job ' + jobDefinition.name);
        xdCommon.$log.info(jobDefinitionService);
        jobDefinitionService.deploy(jobDefinition).$promise.then(
              function () {
                xdCommon.growl.addSuccessMessage('Deployment Request Sent.');
                jobDefinition.deployed = true;
              },
              function (error) {
                xdCommon.$log.error('Error Deploying Job.');
                xdCommon.$log.error(error);
                xdCommon.growl.addErrorMessage('Error Deploying Job.');
              }
            );
      };
      $scope.undeployJob = function (jobDefinition) {
        xdCommon.$log.info('Undeploying Job ' + jobDefinition.name);
        xdCommon.$log.info(jobDefinitionService);
        jobDefinitionService.undeploy(jobDefinition).$promise.then(
              function () {
                xdCommon.growl.addSuccessMessage('Undeployment Request Sent.');
                jobDefinition.deployed = false;
              },
              function (error) {
                xdCommon.$log.error('Error Undeploying Job.');
                xdCommon.$log.error(error);
                xdCommon.growl.addErrorMessage('Error Undeploying Job.');
              }
            );
      };
    }];
});
