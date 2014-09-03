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
        streamService.getModulesFromDSL($stateParams.definitionName, $stateParams.definition).$promise.then(
            function (response) {
              $scope.definitionName = $stateParams.definitionName;
              $scope.definitionDeployRequest = {
                streamDefinition: $stateParams.definition,
                deploymentProperties: {}
              };
              $scope.modules = response;
              response.forEach(function (resp) {
                $scope.definitionDeployRequest.deploymentProperties[resp.moduleLabel] = {};
              });
            },
            function () {
              utils.growl.addErrorMessage('Error getting modules for the stream.');
            }
        );
      });
      $scope.cancelDefinitionDeploy = function () {
        utils.$log.info('Canceling Stream Definition Deployment');
        $state.go('home.streams.tabs.definitions');
      };
      $scope.deployDefinition = function (definitionDeployRequest) {
        utils.$log.info('Deploying Stream Definition ' + definitionDeployRequest);
        utils.$log.info('Deploying Stream Definition ' + definitionDeployRequest.streamDefinition.name);
        utils.$log.info(streamService);
        var deploymentPropertiesString = '';
        var updateDeploymentProperties = function (moduleName, property, value) {
          deploymentPropertiesString += 'module.' + moduleName + '.' + property + '=' + value;
          deploymentPropertiesString += ',';
        };
        for (var deploymentProperties in definitionDeployRequest.deploymentProperties) {
          var value = definitionDeployRequest.deploymentProperties[deploymentProperties];
          if (deploymentProperties === 'count' && (value >= 0 && value !== null)) {
            deploymentPropertiesString += 'module.*.count=' + value;
            deploymentPropertiesString += ',';
          }
          if (deploymentProperties === 'criteria' && value) {
            deploymentPropertiesString += 'module.*.criteria=' + value;
            deploymentPropertiesString += ',';
          }
          for (var properties in value) {
            var propValue = value[properties];
            if ((properties === 'count' && (propValue >= 0 && propValue !== null))) {
              updateDeploymentProperties(deploymentProperties, properties, propValue);
            }
            else if (properties === 'criteria' && propValue) {
              updateDeploymentProperties(deploymentProperties, properties, propValue);
            }
            else if (properties === 'partitionKeyExpression' && propValue) {
              deploymentPropertiesString += 'module.' + deploymentProperties + '.producer.' + properties + '=' + propValue;
              deploymentPropertiesString += ',';
            }
          }
        }
        deploymentPropertiesString = deploymentPropertiesString.substring(0, deploymentPropertiesString.lastIndexOf(','));
        console.log(deploymentPropertiesString);

        utils.$log.info('Deployment Properties:' + deploymentPropertiesString);

        streamService.deploy($scope.definitionName, deploymentPropertiesString).$promise.then(
            function () {
              utils.growl.addSuccessMessage('Deployment Request Sent.');
              $state.go('home.streams.tabs.definitions');
            },
            function (error) {
              utils.growl.addErrorMessage('Error Deploying Stream. ' + error.data[0].message);
            }
        );
      };
    }];
});
