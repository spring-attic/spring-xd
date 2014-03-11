// Karma configuration
// http://karma-runner.github.io/0.10/config/configuration-file.html

module.exports = function (config) {
  'use strict';
  config.set({
    // base path, that will be used to resolve files and exclude
    basePath: '',

    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      'app/lib/angular/angular.js',
      'app/lib/angular-mocks/angular-mocks.js',
      'app/lib/angular-resource/angular-resource.js',
      'app/lib/angular-cookies/angular-cookies.js',
      'app/lib/angular-sanitize/angular-sanitize.js',
      'app/lib/angular-route/angular-route.js',
      'app/lib/angular-ui-router/angular-ui-router.js',
      'app/lib/angular-growl/angular-growl.js',
      'app/lib/angular-promise-tracker/promise-tracker.js',
      'app/lib/angular-busy/angular-busy.js',
      'app/scripts/*.js',
      'app/scripts/**/*.js',
      'test/spec/**/*.js',
      'test/test-main.js'
    ],

    // list of files / patterns to exclude
    exclude: [],

    // web server port
    port: 7070,

    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['PhantomJS'],


    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false
  });
};
