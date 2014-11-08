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
 * XD Containers controller
 *
 * @author Ilayaperumal Gopinathan
 */
define([], function () {
  'use strict';
  return ['$scope', 'ContainerService', 'XDUtils', '$timeout', '$rootScope',
    function ($scope, containerService, utils, $timeout, $rootScope) {
      function loadContainers() {
        containerService.getContainers().$promise.then(
            function (result) {
              utils.$log.info('Retrieved containers...', result);
              var containers = result.content;
              containers.forEach(function (container) {
                if (container.attributes.managementPort && $rootScope.enableMessageRates) {
                  var deployedModules = container.deployedModules;
                  deployedModules.forEach(function (deployedModule) {
                    console.log(deployedModule);
                    if (container.messageRates && container.messageRates[deployedModule.moduleId]) {
                      if (container.messageRates[deployedModule.moduleId].hasOwnProperty('input')) {
                        deployedModule.incomingRate = container.messageRates[deployedModule.moduleId].input.toFixed(5);
                      }
                      if (container.messageRates[deployedModule.moduleId].hasOwnProperty('output')) {
                        deployedModule.outgoingRate = container.messageRates[deployedModule.moduleId].output.toFixed(5);
                      }
                    }
                  });
                }
              });
              $scope.containers = containers;
              loadContainersWithTimeout();
            }
        );
      }
      function loadContainersWithTimeout() {
        $scope.containerTimeOutPromise = $timeout(loadContainers, $rootScope.pageRefreshTime);
      }
      loadContainers();

      $scope.$on('$destroy', function () {
        console.log('Polling cancelled');
        $scope.stopPolling();
      });
      $scope.startPolling = function () {
        console.log('Polling started');
        loadContainersWithTimeout();
      };
      $scope.stopPolling = function () {
        console.log('Polling stopped');
        $timeout.cancel($scope.containerTimeOutPromise);
      };
      $scope.confirmShutdown = function (containerId) {
        $scope.destroyItem = containerId;
      };
      $scope.shutdownContainer = function (containerId) {
        containerService.shutdownContainer(containerId).$promise.then(
            function () {
              utils.growl.success('Shutdown request sent');
              $scope.closeModal();
            },
            function () {
              utils.growl.error('Error shutting down container: ' + containerId);
              $scope.closeModal();
            }
        );
      };
    }];
});
