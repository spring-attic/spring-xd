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
 * Stream Definition controller
 *
 * @author Ilayaperumal Gopinathan
 */
define(['model/pageable'], function (Pageable) {
  'use strict';
  return ['$scope', 'StreamService', 'XDUtils', '$timeout', '$rootScope', '$state',
    function ($scope, streamService, utils, $timeout, $rootScope, $state) {
      $scope.pageable = new Pageable();
      $scope.pagination = {
        current: 1
      };

      $scope.pageChanged = function(newPage) {
        $scope.pageable.pageNumber = newPage-1;
        loadStreamDefinitions($scope.pageable);
      };
      function loadStreamDefinitions(pageable, showGrowl) {
        //utils.$log.info('pageable', pageable);
        var streamDefinitionsPromise = streamService.getDefinitions(pageable).$promise;
        if (showGrowl || showGrowl === undefined) {
          utils.addBusyPromise(streamDefinitionsPromise);
        }
        utils.$log.info(streamDefinitionsPromise);
        streamDefinitionsPromise.then(
          function (result) {
            $scope.pageable.items = result.content;
            $scope.pageable.total = result.page.totalElements;
            var getStreamDefinitions = $timeout(function() {
              loadStreamDefinitions($scope.pageable, false);
            }, $rootScope.pageRefreshTime);
            $scope.$on('$destroy', function(){
              $timeout.cancel(getStreamDefinitions);
            });
          }, function (result) {
            utils.growl.addErrorMessage(result.data[0].message);
          }
        );
      }
      $scope.deployStream = function (streamDefinition) {
        $state.go('home.streams.deployStream', {definitionName: streamDefinition.name});
      };
      $scope.undeployStream = function (streamDefinition) {
        utils.$log.info('Undeploying Stream ' + streamDefinition.name);
        utils.$log.info(streamService);
        streamService.undeploy(streamDefinition).$promise.then(
            function () {
              utils.growl.success('Undeployment Request Sent.');
            },
            function () {
              utils.growl.error('Error Undeploying Stream.');
            }
        );
      };
      $scope.clickModal = function (streamDefinition) {
        $scope.destroyItem = streamDefinition;
      };
      $scope.destroyStream = function (streamDefinition) {
        utils.$log.info('Destroying Stream ' + streamDefinition.name);
        utils.$log.info(streamService);

        streamService.destroy(streamDefinition).$promise.then(
            function () {
              utils.growl.success('Destroy Request Sent.');
              streamDefinition.inactive = true;
              $scope.closeModal();
            },
            function () {
              utils.growl.error('Error Destroying Stream.');
              $scope.closeModal();
            }
        );
      };

      loadStreamDefinitions($scope.pageable);
    }];
});
