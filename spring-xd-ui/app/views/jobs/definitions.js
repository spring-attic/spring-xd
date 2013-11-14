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
 * View for XD Job Definitions.
 * @author Ilayaperumal Gopinathan
 */
/*global define, _, $ */
define([],
function() {
	'use strict';
	return function(Backbone, model, xdJobDefinitionsTemplate, utils, strings) {

		var XDJobDefinitions = Backbone.View.extend({
			initialize: function(options) {
				this.listenTo(model.jobDefinitions, 'change', this.render);
				this.listenTo(model.jobDefinitions, 'reset', this.render);
				model.jobDefinitions.startFetching();
			},

			events: {
				'click .job-deploy': 'handleDeploy',
				'click .job-undeploy': 'handleUndeploy'
			},

			template: _.template(xdJobDefinitionsTemplate),

			render: function() {
				this.$el.html(this.template({jobs: model.jobDefinitions.jobs}));
				return this;
			},

			handleDeploy: function(event) {
				var buttonId = event.target.id;
				var jobName = buttonId.substring(buttonId.indexOf('-')+1);
				var job = model.jobDefinitions.get(jobName);
				if (job) {
					job.deploy().then(function() {
						utils.showSuccessMsg(strings.deployJobSuccess);
						$(buttonId).prop('disabled', true);
						$('undeploy-'+jobName).prop('disabled',false);
					});
				}
			},

			handleUndeploy: function(event) {
				var buttonId = event.target.id;
				var jobName = buttonId.substring(buttonId.indexOf('-')+1);
				var job = model.jobDefinitions.get(jobName);
				if (job) {
					job.undeploy().then(function() {
						utils.showSuccessMsg(strings.undeployJobSuccess);
						$(buttonId).prop('disabled', true);
						$('deploy-'+jobName).prop('disabled',false);
					});
				}
			}
		});
		return XDJobDefinitions;
	};
});
