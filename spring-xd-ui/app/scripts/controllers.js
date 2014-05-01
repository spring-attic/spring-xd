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
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobdefinition'], function (jobDefinitionController) {
              $injector.invoke(jobDefinitionController, this, {'$scope': $scope});
            });
          }])
      .controller('ListJobDeploymentsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobdeployment'], function (jobDeploymentController) {
              $injector.invoke(jobDeploymentController, this, {'$scope': $scope});
            });
          }])
      .controller('ListJobExecutionsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobexecution'], function (jobExecutionController) {
              $injector.invoke(jobExecutionController, this, {'$scope': $scope});
            });
          }])
      .controller('JobExecutionDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobexecutiondetails'], function (jobExecutionDetailsController) {
              $injector.invoke(jobExecutionDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('LoginController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/security/login'], function (loginController) {
              $injector.invoke(loginController, this, {'$scope': $scope});
            });
          }])
      .controller('LogoutController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/security/logout'], function (logoutController) {
              $injector.invoke(logoutController, this, {'$scope': $scope});
            });
          }])
      .controller('ModuleController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobmodule'], function (moduleController) {
              $injector.invoke(moduleController, this, {'$scope': $scope});
            });
          }])
      .controller('JobLaunchController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/joblaunch'], function (jobLaunchController) {
              $injector.invoke(jobLaunchController, this, {'$scope': $scope});
            });
          }])
      .controller('ModuleDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/job/jobmoduledetails'], function (moduleDetailsController) {
              $injector.invoke(moduleDetailsController, this, {'$scope': $scope});
            });
          }]);
});
