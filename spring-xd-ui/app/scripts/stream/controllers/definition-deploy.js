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
 * Definition of the Stream Deployment Details controller
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define([], function () {
  'use strict';
  return ['$scope', 'XDUtils', '$state', '$stateParams', 'StreamService',
    function ($scope, utils, $state, $stateParams, streamService) {
      $scope.$apply(function () {
        $scope.definitionName = $stateParams.definitionName;
        var singleStreamDefinitionPromise = streamService.getSingleStreamDefinition($scope.definitionName);
        utils.$log.info(singleStreamDefinitionPromise);
        utils.addBusyPromise(singleStreamDefinitionPromise);
        singleStreamDefinitionPromise.then(
          function (result) {
            utils.$log.info(result);
            var streamDefinition = result.data;
            if (!streamDefinition) {
              utils.growl.error('No valid stream definition returned for definition name ' + $scope.definitionName, streamDefinition);
              return;
            }
            $scope.definitionDeployRequest = {
              streamDefinition: streamDefinition,
              deploymentProperties: ''
            };
          },function (error) {
            utils.growl.error('Error fetching stream definition. ' + error.data[0].message);
          });
      });
      $scope.cancelDefinitionDeploy = function () {
        utils.$log.info('Canceling Stream Definition Deployment');
        $state.go('home.streams.tabs.definitions');
      };
      $scope.deployDefinition = function (definitionDeployRequest) {
        utils.$log.info('Deploying Stream Definition ' + definitionDeployRequest.streamDefinition.name, definitionDeployRequest);
        var properties = definitionDeployRequest.deploymentProperties;

        utils.$log.info('Deployment Properties:', properties);

        streamService.deploy(definitionDeployRequest.streamDefinition, properties).$promise.then(
            function () {
              utils.growl.success('Deployment Request Sent.');
              $state.go('home.streams.tabs.definitions');
            },
            function (error) {
              utils.growl.error('Error Deploying Stream. ' + error.data[0].message);
            }
          );
      };
    }];
});
