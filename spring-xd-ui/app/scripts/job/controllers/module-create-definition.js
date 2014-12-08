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
 * Create a Job Definition from a Module
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', 'JobModuleService', 'XDUtils', '$state', '$stateParams',
    function ($scope, jobModuleService, utils, $state, $stateParams) {
      $scope.$apply(function () {

        $scope.moduleName = $stateParams.moduleName;

        var singleModulePromise = jobModuleService.getSingleModule($stateParams.moduleName).$promise;
        utils.addBusyPromise(singleModulePromise);

        singleModulePromise.then(
            function (result) {
                $scope.jobDefinition = {
                  name: '',
                  deploy: true,
                  parameters: []
                };
                var arrayLength = result.options.length;
                for (var i = 0; i < arrayLength; i++) {
                  var option = result.options[i];
                  var optionValue = option.defaultValue ? option.defaultValue : '';

                  $scope.jobDefinition.parameters.push({
                    name: option.name,
                    value: optionValue,
                    type: option.type,
                    description: option.description
                  });
                }
                $scope.moduleDetails = result;
              }, function () {
                utils.growl.error('Error fetching data. Is the XD server running?');
              });
        $scope.closeCreateDefinition = function () {
            utils.$log.info('Closing Job Definition Creation Window');
            $state.go('home.jobs.tabs.modules');
          };
        $scope.submitJobDefinition = function () {
            utils.$log.info('Submitting Definition');
            calculateDefinition($scope.jobDefinition);
            var createDefinitionPromise = jobModuleService.createDefinition(
                    $scope.jobDefinition.name, $scope.calculatedDefinition, $scope.jobDefinition.deploy).$promise;
            utils.addBusyPromise(createDefinitionPromise);
            createDefinitionPromise.then(
                    function () {
                      utils.growl.success('The Definition was created.');
                      $state.go('home.jobs.tabs.modules');
                    }, function (error) {
                      utils.growl.error(error.data[0].message);
                    });
          };
        $scope.$watch('jobDefinition', function() {
          if ($scope.jobDefinition) {
            calculateDefinition($scope.jobDefinition);
          }
        }, true);

        function calculateDefinition(jobDefinition) {
          var arrayLength = jobDefinition.parameters.length;
          $scope.calculatedDefinition = $scope.moduleDetails.name;
          for (var i = 0; i < arrayLength; i++) {
            var parameter = jobDefinition.parameters[i];
            if (parameter.value) {
              var parameterValueToUse = escapeStringIfNecessary(parameter.name, parameter.value);
              $scope.calculatedDefinition = $scope.calculatedDefinition + ' --' + parameter.name + '=' + parameterValueToUse;
            }
          }
        }
        function escapeStringIfNecessary(name, value) {
          if (value && /\s/g.test(value)) {
            return '"' + value + '"';
          }
          else if (name === 'password' || name === 'passwd') {
            return '"' + value + '"';
          }
          else {
            return value;
          }
        }
      });
    }];
});
