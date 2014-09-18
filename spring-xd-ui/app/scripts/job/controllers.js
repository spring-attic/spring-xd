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

  return angular.module('xdJobsAdmin.controllers', [])
      .controller('JobDefinitionsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/definitions'], function (jobDefinitionsController) {
              $injector.invoke(jobDefinitionsController, this, {'$scope': $scope});
            });
          }])
      .controller('JobDefinitionDeployController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/definition-deploy'], function (jobDefinitionDeployController) {
              $injector.invoke(jobDefinitionDeployController, this, {'$scope': $scope});
            });
          }])
      .controller('JobDeploymentsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/deployments'], function (jobDeploymentsController) {
              $injector.invoke(jobDeploymentsController, this, {'$scope': $scope});
            });
          }])
      .controller('JobDeploymentDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/deployment-details'], function (jobDeploymentDetailsController) {
              $injector.invoke(jobDeploymentDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('JobExecutionsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/executions'], function (jobExecutionsController) {
              $injector.invoke(jobExecutionsController, this, {'$scope': $scope});
            });
          }])
      .controller('JobExecutionDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/execution-details'], function (jobExecutionDetailsController) {
              $injector.invoke(jobExecutionDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('StepExecutionDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/stepexecutiondetails'], function (stepExecutionDetailsController) {
              $injector.invoke(stepExecutionDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('StepExecutionProgressController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/stepexecutionprogress'], function (stepExecutionDetailsController) {
              $injector.invoke(stepExecutionDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('JobLaunchController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/launch'], function (jobLaunchController) {
              $injector.invoke(jobLaunchController, this, {'$scope': $scope});
            });
          }])
      .controller('JobScheduleController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/schedule'], function (jobScheduleController) {
              $injector.invoke(jobScheduleController, this, {'$scope': $scope});
            });
          }])
      .controller('ModuleController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/modules'], function (modulesController) {
              $injector.invoke(modulesController, this, {'$scope': $scope});
            });
          }])
      .controller('ModuleDetailsController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/module-details'], function (moduleDetailsController) {
              $injector.invoke(moduleDetailsController, this, {'$scope': $scope});
            });
          }])
      .controller('ModuleCreateDefinitionController',
          ['$scope', '$injector', function ($scope, $injector) {
            require(['job/controllers/module-create-definition'], function (moduleCreateDefinitionController) {
              $injector.invoke(moduleCreateDefinitionController, this, {'$scope': $scope});
            });
          }]);
});
