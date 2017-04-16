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
 * Handles the listing of job modules.
 *
 * @author Gunnar Hillert
 */
define(['model/pageable'], function (Pageable) {
  'use strict';

  return ['$scope', 'JobModuleService', 'XDUtils', '$state',
    function ($scope, jobModuleService, utils, $state) {
      $scope.pageable = new Pageable();
      $scope.pagination = {
        current: 1
      };
      $scope.pageChanged = function(newPage) {
        $scope.pageable.pageNumber = newPage-1;
        getJobModules($scope.pageable);
      };
      function getJobModules(pageable) {
        utils.$log.info('pageable', pageable);
        var jobModulesPromise = jobModuleService.getAllModules(pageable).$promise;
        utils.addBusyPromise(jobModulesPromise);
        jobModulesPromise.then(
          function (result) {
            utils.$log.info(result);
            $scope.pageable.items = result.content;
            $scope.pageable.total = result.page.totalElements;
          }, function (result) {
            utils.growl.error(result.data[0].message);
          }
        );
      }
      $scope.viewModuleDetails = function (item) {
        utils.$log.info('Showing Module details for module: ' + item.name);
        $state.go('home.jobs.moduledetails', {moduleName: item.name});
      };
      $scope.createDefinition = function (item) {
        $state.go('home.jobs.modulecreatedefinition', {moduleName: item.name});
      };

      getJobModules($scope.pageable);
    }
  ];
});
