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
 * RequireJS configuration and bootstrapping angular.
 *
 * @author Ilayaperumal Gopinathan
 */
require.config({
  paths: {
    model:   'shared/model',
    domReady: '../lib/requirejs-domready/domReady',
    angular: '../lib/angular/angular',
    jquery: '../lib/jquery/jquery',
    bootstrap: '../lib/bootstrap/bootstrap',
    ngResource: '../lib/angular-resource/angular-resource',
    uiRouter: '../lib/angular-ui-router/angular-ui-router',
    cgBusy: '../lib/angular-busy/angular-busy',
    ngGrowl: '../lib/angular-growl-v2/angular-growl',
    ngAnimate: '../lib/angular-animate/angular-animate',
    angularHighlightjs: '../lib/angular-highlightjs/angular-highlightjs',
    highlightjs: '../lib/highlightjs/highlight.pack',
    xregexp: '../lib/xregexp/xregexp-all',
    pagination: '../lib/angular-utils-pagination/dirPagination',
    moment: '../lib/moment/moment'
  },
  shim: {
    angular: {
      deps: ['bootstrap'],
      exports: 'angular'
    },
    bootstrap: {
      deps: ['jquery']
    },
    'uiRouter': {
      deps: ['angular']
    },
    'ngResource': {
      deps: ['angular']
    },
    'pagination': {
      deps: ['angular']
    },
    'ngAnimate': {
      deps: ['angular']
    },
    'cgBusy': {
      deps: ['angular']
    },
    'ngGrowl': {
      deps: ['angular', 'ngAnimate']
    },
    'xregexp': {
      deps: []
    },
    'angularHighlightjs': {
      deps: ['angular', 'highlightjs']
    }
  }
});

define([
  'require',
  'angular'
], function (require, angular) {
  'use strict';

  var app = angular.module('xdConf', []);

  var initInjector = angular.injector(['ng']);
  var $http = initInjector.get('$http');
  var securityInfoUrl = '/security/info';
  var timeout = 20000;
  var promiseHttp = $http.get(securityInfoUrl, {timeout: timeout});

  promiseHttp.then(function(response) {
    app.constant('securityInfo', response.data);
    require(['app', './routes'], function () {
      require(['domReady!'], function (document) {
        console.log('Start angular application.');
        angular.bootstrap(document, ['xdAdmin']);
      });
    });
  }, function(errorResponse) {
    var errorMessage = 'Error retrieving security info from ' + securityInfoUrl + ' (timeout: ' + timeout + 'ms)';
    console.log(errorMessage, errorResponse);
    $('.splash .container').html(errorMessage);
  });

  require(['jquery', 'bootstrap'], function () {
    console.log('Loaded Twitter Bootstrap.');
    updateGrowl();
    $(window).on('scroll resize', function () {
      updateGrowl();
    });
  });

  function updateGrowl() {
    var bodyScrollTop = $(document).scrollTop();
    var navHeight = $('nav').outerHeight();
    var marginToParent = 10;

    if ($(window).width() <= 768) {
      marginToParent = 0;
    }

    if (bodyScrollTop > navHeight) {
      $('.growl-container').css('top', marginToParent);
    } else if (bodyScrollTop >= 0) {
      var distance = navHeight - bodyScrollTop;
      $('.growl-container').css('top', distance + marginToParent);
    }
  }
});
