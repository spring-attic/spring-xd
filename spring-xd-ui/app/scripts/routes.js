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
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
define(['./app'], function (xdAdmin) {
  'use strict';
  xdAdmin.config(function ($stateProvider, $urlRouterProvider, $httpProvider) {
    $httpProvider.defaults.useXDomain = true;

    $urlRouterProvider.otherwise('/jobs/definitions');

    $stateProvider.state('home', {
      url : '/',
      abstract:true,
      templateUrl : 'views/home.html'
    })
    .state('home.jobs', {
      url : 'jobs',
      abstract:true,
      templateUrl : 'views/jobs/jobs.html'
    })
    .state('home.about', {
      url : 'about',
      templateUrl : 'views/about.html'
    })
    .state('home.jobs.definitions', {
      url : '/definitions',
      templateUrl : 'views/jobs/definitions.html',
      controller: 'ListDefinitionController'
    })
    .state('home.jobs.deployments', {
      url : '/deployments',
      templateUrl : 'views/jobs/deployments.html',
      controller: 'ListJobDeploymentsController'
    })
    .state('home.jobs.executions', {
      url : '/executions',
      templateUrl : 'views/jobs/executions.html',
      controller: 'ListJobExecutionsController'
    })
    .state('home.jobs.deployments.launch', {
      url : '/launch/{jobName}',
      templateUrl : 'views/jobs/launch.html',
      controller: 'JobLaunchController'
    });
  });
  xdAdmin.run(function ($rootScope, $state, $stateParams) {
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
    $rootScope.xdAdminServerUrl = window.location.protocol + '//' + window.location.host;
  });
  return xdAdmin;
});
