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
  return ['$scope', 'JobDefinitions', 'JobDefinitionService', 'Helper',
    function ($scope, jobDefinitions, jobDefinitionService, Helper) {

      jobDefinitions.get(function (data) {
        Helper.$log.info(data);
        $scope.jobDefinitions = data.content;
      }, function (error) {
        Helper.$log.error('Error fetching data. Is the XD server running?');
        Helper.$log.error(error);
        Helper.growl.addErrorMessage('Error fetching data. Is the XD server running?');
      });

      $scope.deployJob = function (jobDefinition) {
        Helper.$log.info('Deploying Job ' + jobDefinition.name);
        Helper.$log.info(jobDefinitionService);
        jobDefinitionService.deploy(jobDefinition).$promise.then(
              function () {
                Helper.growl.addSuccessMessage('Deployment Request Sent.');
                jobDefinition.deployed = true;
              },
              function (error) {
                Helper.$log.error('Error Deploying Job.');
                Helper.$log.error(error);
                Helper.growl.addErrorMessage('Error Deploying Job.');
              }
            );
      };
      $scope.undeployJob = function (jobDefinition) {
        Helper.$log.info('Undeploying Job ' + jobDefinition.name);
        Helper.$log.info(jobDefinitionService);
        jobDefinitionService.undeploy(jobDefinition).$promise.then(
              function () {
                Helper.growl.addSuccessMessage('Undeployment Request Sent.');
                jobDefinition.deployed = false;
              },
              function (error) {
                Helper.$log.error('Error Undeploying Job.');
                Helper.$log.error(error);
                Helper.growl.addErrorMessage('Error Undeploying Job.');
              }
            );
      };
    }];
});
