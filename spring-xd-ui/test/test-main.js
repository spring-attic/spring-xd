var tests = [];
for (var file in window.__karma__.files) {
  if (window.__karma__.files.hasOwnProperty(file)) {
    if (/spec\.js$/i.test(file)) {
      tests.push(file);
    }
  }
}

require.config({
  paths: {
    angular: '/base/app/lib/angular/angular',
    angularRoute: '/base/app/lib/angular-route/angular-route',
    angularMocks: '/base/app/lib/angular-mocks/angular-mocks',
    ngResource: '/base/app/lib/angular-resource/angular-resource',
    text: '/base/app/lib/requirejs-text/text',
    fixtures: '/base/app/test/spec/fixtures',
    angularHighlightjs: '/base/app/lib/angular-highlightjs/angular-highlightjs',
    highlightjs: '/base/app/lib/highlightjs/highlight.pack',
    uiRouter: '/base/app/lib/angular-ui-router/angular-ui-router',
    cgBusy: '/base/app/lib/angular-busy/angular-busy',
    ngGrowl: '/base/app/lib/angular-growl/angular-growl',
    ngAnimate: '/base/app/lib/angular-animate/angular-animate',
    lodash: '/base/app/lib/lodash/lodash.compat',
    xregexp: '/base/app/lib/xregexp/xregexp-all'
  },
  baseUrl: '/base/app/scripts',
  shim: {
    'angular' : {'exports' : 'angular'},
    'angularRoute': ['angular'],
    'angularMocks': {
      deps:['angular'],
      'exports':'angular.mock'
    },
    'uiRouter': {
       deps: ['angular']
    },
    cgBusy: {
      deps: ['angular']
    },
    lodash: {},
    'angularHighlightjs': {
      deps: ['angular', 'highlightjs']
    },
    'ngResource': {
      deps: ['angular']
    },
    'ngAnimate': {
        deps: ['angular']
    },
    'ngGrowl': {
      deps: ['angular', 'ngAnimate']
    },
  },
  deps: tests,
  callback: window.__karma__.start
});

