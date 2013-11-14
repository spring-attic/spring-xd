/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */
/*global define, window, $*/
// TODO move all these module parameters into the anonymous function
define([], function() {
	return function(utils, JobDefinitions, JobDeployments, JobExecutions, router) {
		var views = {};

		// The wire.js init function
		// called after initialization
		// important dom nodes have been injected
		views.init = function() {
			// Initialize the views
			// Job Definitions tab view
			views.jobDefinitions = new JobDefinitions({
				el: this.jobDefinitionsElt
			});
			// Job Deployments tab view
			views.jobDeployments = new JobDeployments({
				el: this.jobDeploymentsElt
			});
			// Job Executions tab view
			views.jobExecutions = new JobExecutions({
				el: this.jobExecutionsElt
			});
			//Initialize DOM elements for better handling
			var loading = $(this.xdLoadingElt);
			var statusMsg = $(this.xdStatusMsgElt);

			$(window.document).on('success', function(evt, message) {
				statusMsg.show();
				statusMsg.html('');
				statusMsg.show();
				statusMsg.html(utils.updateStatus("Success", message));
			});
			$(window.document).on('error', function(evt, message) {
				statusMsg.show();
				statusMsg.html('');
				statusMsg.show();
				statusMsg.html(utils.updateStatus("Error", message));
			});

			views.done = function() {
				loading.hide();
				statusMsg.hide();
			};
			views.done();
			// hook the views up to router functions
			//router.initializeViewRouting(views);
		};

		return views;
	};
});
