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
/*jslint browser:true */
/*global jasmine require requirejs*/

requirejs.config({
	baseUrl: "../app",
	packages: [
		{ name: 'rest', location: '../lib/rest', main:'rest'},
		{ name: 'when', location: '../lib/when', main:'when'}
	],
	paths: {
		jquery: '../lib/jquery/jquery',
		underscore: '../lib/lodash/lodash',
		'text': '../lib/requirejs-text/text',
		'bootstrap-typeahead': '../lib/bootstrap/js/bootstrap-typeahead',
		'bootstrap-tab': '../lib/bootstrap/js/bootstrap-tab',
		'bootstrap-alert': '../lib/bootstrap/js/bootstrap-alert',
		'bootstrap-collapse': '../lib/bootstrap/js/bootstrap-collapse',
		'cubism': '../lib/cubism/cubism.v1',
		'd3': '../lib/d3/d3',
		'tipsy': '../lib/tipsy/jquery.tipsy',
		backbone: '../lib/backbone-amd/backbone',
		templates: '../templates'
	},
	shim: {
		'bootstrap-typeahead': ['jquery'],
		'bootstrap-alert': ['jquery'],
		'bootstrap-collapse': ['jquery'],
		'bootstrap-tab': ['jquery'],
		'tipsy': ['jquery'],
		'cubism': ['d3']
	}
});

require(['../test/spec/xd.model.spec', '../test/spec/xd.viewer.spec'],
function(model_spec, viewer_spec) {
	var jasmineEnv = jasmine.getEnv();
	jasmineEnv.updateInterval = 1000;

	var htmlReporter = new jasmine.HtmlReporter();
	if (jasmine.hasOwnProperty("ConsoleReporter")) {
		var consoleReporter = new jasmine.ConsoleReporter();
		jasmineEnv.addReporter(consoleReporter);
    }
    jasmineEnv.addReporter(htmlReporter);
	jasmineEnv.specFilter = function(spec) {
		return htmlReporter.specFilter(spec);
	};
	jasmineEnv.execute();
});
