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
  return ['$scope', 'RuntimeContainerService', 'XDUtils', '$timeout', '$rootScope',
    function ($scope, runtimeContainerService, utils, $timeout, $rootScope) {

      (function loadRuntimeContainers() {
        runtimeContainerService.getRuntimeContainers().$promise.then(
            function (result) {
              utils.$log.info(result);
              var containers = result.content;
//              containers.forEach(function (runtimeContainer) {
//                if (runtimeContainer.attributes.managementPort) {
//                  var deployedModules = runtimeContainer.deployedModules;
//                  deployedModules.forEach(function (deployedModule) {
//                    console.log(deployedModule);
//                    if (runtimeContainer.messageRates[deployedModule.moduleId]) {
//                      if (runtimeContainer.messageRates[deployedModule.moduleId].hasOwnProperty('input')) {
//                        deployedModule.incomingRate = runtimeContainer.messageRates[deployedModule.moduleId].input.toFixed(5);
//                      }
//                      if (runtimeContainer.messageRates[deployedModule.moduleId].hasOwnProperty('output')) {
//                        deployedModule.outgoingRate = runtimeContainer.messageRates[deployedModule.moduleId].output.toFixed(5);
//                      }
//                    }
//                  });
//                }
//              });
              $scope.runtimeContainers = containers;

              var getRuntimeContainers = $timeout(loadRuntimeContainers, $rootScope.pageRefreshTime);
              $scope.$on('$destroy', function () {
                $timeout.cancel(getRuntimeContainers);
              });
            }
        );
      })();
      $scope.shutdownContainer = function (containerId) {
        runtimeContainerService.shutdownContainer(containerId).$promise.then(
            function () {
              utils.growl.addSuccessMessage('Shutdown request sent');
            },
            function () {
              utils.growl.addErrorMessage('Error shutting down container: ' + containerId);
            }
        );
      };
    }];
});
