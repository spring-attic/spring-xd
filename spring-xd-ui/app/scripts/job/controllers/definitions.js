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
define(['model/pageable'], function (Pageable) {
  'use strict';
  return ['$scope', 'JobDefinitions', 'JobDefinitionService', 'XDUtils', '$state', '$timeout', '$rootScope',
    function ($scope, jobDefinitions, jobDefinitionService, utils, $state, $timeout, $rootScope) {
      $scope.pageable = new Pageable();
      $scope.pagination = {
        current: 1
      };
      $scope.pageChanged = function(newPage) {
        $scope.pageable.pageNumber = newPage-1;
        loadJobDefinitions($scope.pageable);
      };

      function loadJobDefinitions(pageable, showGrowl) {
        utils.$log.info('pageable', pageable);
        var jobDefinitionsPromise = jobDefinitions.getAllJobDefinitions(pageable).$promise;
        jobDefinitionsPromise.then(
            function (result) {
              utils.$log.info(result);
              $scope.pageable.items = result.content;
              $scope.pageable.total = result.page.totalElements;

              var getJobDefinitions = $timeout(function() {
                loadJobDefinitions($scope.pageable, false);
              }, $rootScope.pageRefreshTime);
              $scope.$on('$destroy', function(){
                $timeout.cancel(getJobDefinitions);
              });
            }
        );
        if (showGrowl || showGrowl === undefined) {
          utils.addBusyPromise(jobDefinitionsPromise);
        }
      }
      $scope.deployJob = function (jobDefinition) {
        $state.go('home.jobs.deployjob', {definitionName: jobDefinition.name});
      };
      $scope.undeployJob = function (jobDefinition) {
        utils.$log.info('Undeploying Job ' + jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        jobDefinitionService.undeploy(jobDefinition).$promise.then(
            function (data) {
              console.log(data);
              utils.growl.success('Undeployment Request Sent.');
            },
            function () {
              utils.growl.error('Error Undeploying Job.');
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
              utils.growl.success('Destroy Request Sent.');
              jobDefinition.inactive = true;
              $scope.closeModal();
            },
            function () {
              utils.growl.error('Error Destroying Job.');
              $scope.closeModal();
            }
        );
      };
      loadJobDefinitions($scope.pageable);
    }];
});
