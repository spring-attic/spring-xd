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
 * Definition of xdAdmin services.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdAdmin.services', [])
      .factory('JobDefinitions', function ($resource, $rootScope) {
        return $resource($rootScope.xdAdminServerUrl + '/jobs.json?deployments=true', {}, {
          query: {
            method: 'GET',
            isArray: true
          }
        });
      })
      .factory('JobModules', function ($resource, $rootScope) {
        return $resource($rootScope.xdAdminServerUrl + '/modules.json?type=job', {}, {
          query: {
            method: 'GET',
            isArray: true
          }
        });
      })
      .factory('JobModuleService', function ($resource, $http, $log, $rootScope) {
        return {
          getAllModules: function () {
            $log.info('Getting all job modules.');
            return $resource($rootScope.xdAdminServerUrl + '/modules.json', { 'type': 'job' }).get();
          },
          getSingleModule: function (moduleName) {
            $log.info('Getting details for module ' + moduleName);
            return $resource($rootScope.xdAdminServerUrl + '/modules/job/' + moduleName + '.json').get();
          },
          getModuleDefinition: function (moduleName) {
            $log.info('Getting module definition file for module ' + moduleName);
            return $http({
              method: 'GET',
              url: $rootScope.xdAdminServerUrl + '/modules/job/' + moduleName + '/definition'
            });
          }
        };
      })
      .factory('JobDefinitionService', function ($resource, $log, $rootScope) {
        return {
          deploy: function (jobDefinition) {
            $log.info('Deploy Job ' + jobDefinition.name);
            return $resource($rootScope.xdAdminServerUrl + '/jobs/' + jobDefinition.name, { 'deploy': true }, {
              deploy: { method: 'PUT' }
            }).deploy();
          },
          undeploy: function (jobDefinition) {
            $log.info('Undeploy Job ' + jobDefinition.name);
            return $resource($rootScope.xdAdminServerUrl + '/jobs/' + jobDefinition.name, { 'deploy': false }, {
              undeploy: { method: 'PUT' }
            }).undeploy();
          }
        };
      })
      .factory('JobDeployments', function ($resource, $rootScope) {
        return $resource($rootScope.xdAdminServerUrl + '/batch/jobs.json', {}, {
          getArray: {method: 'GET', isArray: true}
        });
      })
      .factory('JobExecutions', function ($resource, $rootScope, $log) {
        return {
          getArray: function () {
            $log.info('Get Job Executions ');
            return $resource($rootScope.xdAdminServerUrl + '/batch/executions', {}, {
              getArray: {method: 'GET', isArray: true}
            }).getArray();
          },
          getSingleJobExecution: function (jobExecutionId) {
            $log.info('Getting details for Job Execution with Id ' + jobExecutionId);
            return $resource($rootScope.xdAdminServerUrl + '/batch/executions/' + jobExecutionId + '.json').get();
          },
          restart: function (jobExecution) {
            $log.info('Restart Job Execution' + jobExecution.executionId);
            return $resource($rootScope.xdAdminServerUrl + '/batch/executions/' + jobExecution.executionId, { 'restart': true }, {
              restart: { method: 'PUT' }
            }).restart();
          }
        };
      })
      .factory('StepExecutions', function ($resource, $rootScope, $log) {
        return {
          getSingleStepExecution: function (jobExecutionId, stepExecutionId) {
            $log.info('Getting details for Step Execution with Id ' + stepExecutionId + '(Job Execution Id ' + jobExecutionId + ')');
            return $resource($rootScope.xdAdminServerUrl + '/batch/executions/' + jobExecutionId +  '/steps/' + stepExecutionId + '.json').get();
          }
        };
      })
      .factory('JobLaunchService', function ($resource, growl, $rootScope) {
        return {
          convertToJsonAndSend: function (jobLaunchRequest) {

            console.log('Converting to Json: ' + jobLaunchRequest.jobName);
            console.log(jobLaunchRequest);
            //console.log('Model Change: ' + JSON.stringify(jobLaunchRequest.toJSON()));

            var jsonData = {};
            jobLaunchRequest.jobParameters.forEach(function (jobParameter) {

              var key = jobParameter.key;
              var value = jobParameter.value;
              var isIdentifying = jobParameter.isIdentifying;
              var dataType = jobParameter.type;
              var dataTypeToUse = '';

              if (typeof dataType !== 'undefined') {
                dataTypeToUse = '(' + dataType + ')';
              }

              if (isIdentifying) {
                jsonData['+' + key + dataTypeToUse] = value;
              }
              else {
                jsonData['-' + key + dataTypeToUse] = value;
              }
            });
            console.log(jsonData);
            var jsonDataAsString = JSON.stringify(jsonData);

            console.log(jsonDataAsString);

            this.launch(jobLaunchRequest.jobName, jsonDataAsString);
          },
          launch: function (jobName, jsonDataAsString) {
            console.log('Do actual Launch...');
            $resource($rootScope.xdAdminServerUrl + '/jobs/' + jobName + '/launch', { 'jobParameters': jsonDataAsString }, {
              launch: { method: 'PUT' }
            }).launch().$promise.then(
                function () {
                  growl.addSuccessMessage('Job ' + jobName + ' launched.');
                },
                function (data) {
                  console.error(data);
                  growl.addErrorMessage('Yikes, something bad happened while launching job ' + jobName);
                  growl.addErrorMessage(data.data[0].message);
                }
            );
          }
        };
      })
      .factory('User', function() {
          var sdo = {
            isAuthenticated: false,
            username: ''
          };
          return sdo;
        })
      .factory('XDCommon', function($log, growl, $timeout, $q, $rootScope) {
          return {
            $log: $log,
            growl: growl,
            $timeout: $timeout,
            $q: $q,
            $rootScope: $rootScope,
            addBusyPromise: function(promise) {
              $rootScope.cgbusy = promise;
            },
          };
        });
});


