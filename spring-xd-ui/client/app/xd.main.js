/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @author Andrew Eisenberg
 */


/*global requirejs d3, cubism*/

requirejs.config({
	packages: [
		{ name: 'rest', location: '../bower_components/rest', main:'rest'},
		{ name: 'when', location: '../bower_components/when', main:'when'}
	],
	paths: {
		jquery: '../bower_components/jquery/jquery',
		underscore: '../bower_components/lodash/lodash',
		'bootstrap-typeahead': '../bower_components/bootstrap/js/bootstrap-typeahead',
		'bootstrap-tab': '../bower_components/bootstrap/js/bootstrap-tab',
		'bootstrap-alert': '../bower_components/bootstrap/js/bootstrap-alert',
		'bootstrap-collapse': '../bower_components/bootstrap/js/bootstrap-collapse',
		'cubism': '../bower_components/cubism/cubism.v1',
		'd3': '../bower_components/d3/d3',
		backbone: '../bower_components/backbone-amd/backbone'
	},
    shim: {
        'bootstrap-typeahead': ['jquery'],
        'bootstrap-alert': ['jquery'],
        'bootstrap-collapse': ['jquery'],
        'bootstrap-tab': ['jquery'],
        'cubism': ['d3']
    }
});

require(['backbone', 'xd.router', 'xd.viewer',  'xd.model', 'd3', 'cubism', 'bootstrap-typeahead', 'bootstrap-tab', 'bootstrap-alert', 'bootstrap-collapse'],
function(Backbone, router, viewer, model) {
    new router.xdRouter(viewer);
    Backbone.history.start();
});
