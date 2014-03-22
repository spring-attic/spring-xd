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
      data:{
        authenticate: true
      },
      templateUrl : 'views/jobs/jobs.html'
    })
    .state('home.about', {
      url : 'about',
      templateUrl : 'views/about.html',
      data:{
        authenticate: false
      }
    })
    .state('login', {
      url : '/login',
      controller: 'LoginController',
      templateUrl : 'views/login.html',
      data:{
        authenticate: false
      }
    })
    .state('logout', {
      url : '/logout',
      controller: 'LogoutController',
      templateUrl : 'views/login.html',
      data:{
        authenticate: true
      }
    })
    .state('home.jobs.modules', {
      url : '/modules',
      templateUrl : 'views/jobs/modules.html',
      controller: 'ModuleController'
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
      //TODO: Once ScheduleJobsController is available we can have the routes configured.
//    .state('home.jobs.scheduledJobs', {
//      url : '/scheduled-jobs',
//      templateUrl : 'views/jobs/scheduledJobs.html',
//      controller: 'ScheduledJobsController'
//    })
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
  xdAdmin.run(function ($rootScope, $state, $stateParams, User, $log) {
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
    $rootScope.xdAdminServerUrl = window.location.protocol + '//' + window.location.host;
    $rootScope.authenticationEnabled = false;
    $rootScope.user = User;

    $rootScope.$on('$stateChangeStart', function(event, toState) {
        $log.info('Need to authenticate? ' + toState.data.authenticate);
        if ($rootScope.authenticationEnabled && toState.data.authenticate && !User.isAuthenticated){
          // User is not authenticated
          $state.transitionTo('login');
          event.preventDefault();
        }
      });
  });
  return xdAdmin;
});
