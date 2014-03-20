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
 * Definition of xdAdmin controllers.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdAdmin.controllers', ['xdAdmin.services'])
      .controller('ListDefinitionController',
          ['$scope', '$http', 'JobDefinitions', '$log', 'promiseTracker', '$q', '$timeout', 'growl', 'JobDefinitionService', '$injector',
            function ($scope, $http, JobDefinitions, $log, promiseTracker, $q, $timeout, growl, JobDefinitionService, $injector) {
              require(['controllers/job/jobdefinition'], function (jobDefinitionController) {
                $injector.invoke(jobDefinitionController, this,
                    {'$scope': $scope, '$http': $http, 'JobDefinitions': JobDefinitions, $log: $log,
                      'promiseTracker': promiseTracker, '$q': $q, '$timeout': $timeout,
                      'growl': growl, 'JobDefinitionService': JobDefinitionService });
              });
            }])
      .controller('ListJobDeploymentsController',
          ['$scope', '$http', 'JobDeployments', '$log', '$state', 'growl', '$injector',
            function ($scope, $http, JobDeployments, $log, $state, growl, $injector) {
              require(['controllers/job/jobdeployment'], function (jobDeploymentController) {
                $injector.invoke(jobDeploymentController, this,
                    {'$scope': $scope, '$http': $http, 'JobDeployments': JobDeployments, $log: $log,
                      '$state': $state, 'growl': growl});
              });
            }])
      .controller('ListJobExecutionsController',
          ['$scope', '$http', 'JobExecutions', '$log', 'growl', '$injector', function ($scope, $http, JobExecutions, $log, growl, $injector) {
            require(['controllers/job/jobexecution'], function (jobExecutionController) {
              $injector.invoke(jobExecutionController, this,
                  {'$scope': $scope, '$http': $http, 'JobExecutions': JobExecutions, $log: $log, 'growl': growl});
            });
          }])
      .controller('LoginController',
          ['$scope', '$http', 'User', '$state', 'growl', '$injector', function ($scope, $http, User, $state, growl, $injector) {
              require(['controllers/security/login'], function (loginController) {
                $injector.invoke(loginController, this,
                    {'$scope': $scope, '$http': $http, 'User': User, '$state': $state, 'growl': growl});
              });
            }])
      .controller('LogoutController',
          ['$scope', '$http', 'User', '$state', 'growl', '$injector', function ($scope, $http, User, $state, growl, $injector) {
              require(['controllers/security/logout'], function (logoutController) {
                $injector.invoke(logoutController, this,
                    {'$scope': $scope, '$http': $http, 'User': User, '$state': $state, 'growl': growl});
              });
            }])
      .controller('JobLaunchController',
          ['$scope', '$http', '$log', '$state', '$stateParams', 'growl', '$location', 'JobLaunchService', '$injector', function ($scope, $http, $log, $state, $stateParams, growl, $location, JobLaunchService, $injector) {
            require(['controllers/job/joblaunch'], function (jobLaunchController) {
              $injector.invoke(jobLaunchController, this,
                  {'$scope': $scope, '$http': $http, '$state': $state, '$stateParams': $stateParams, 'growl': growl, '$location': $location, 'JobLaunchService': JobLaunchService});
            });
          }]);
});
