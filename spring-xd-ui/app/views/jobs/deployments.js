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
 * View for XD Job deployments
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */
/*global define, _, $ */
define([],
function() {
	'use strict';
	return function(Backbone, model, xdJobDeploymentsTemplate, jobParamsFormTemplate, utils, strings) {

		function extractJob(name) {
			return model.batchJobs.get(name);
        }

		var XDJobDeployments = Backbone.View.extend({
			initialize: function(options) {
				this.listenTo(model.batchJobs, 'change', this.render);
				this.listenTo(model.batchJobs, 'reset', this.render);
				model.batchJobs.startFetching();
			},

			events: {
				'click button.open-job-params': 'openJobParamsModal',
				'click button.job-schedule': 'scheduleJob',
				'click button.add-job-param': 'addJobParamToModal'
			},

			template: _.template(xdJobDeploymentsTemplate),

			jobParamsFormTemplate: _.template(jobParamsFormTemplate),

			render: function() {
				this.$el.html(this.template({jobs: model.batchJobs.jobs}));
				return this;
			},

			openJobParamsModal: function(event) {
				var launchId = event.target.id;
				var jobName = launchId.substring(launchId.indexOf('-')+1);
				//TODO: use jobName when launching job from Modal
				$('#job-params-modal').modal('show');
			},

			addJobParamToModal: function() {
				$('#job-params-form').append(this.jobParamsFormTemplate());
			},

//			launchJob: function(event) {
//				var job = extractJob(jobName);
//				if (job) {
//                    job.launch().then(function() {
//                        utils.showSuccessMsg(strings.launchJobSuccess);
//                    });
//                }
//			},

			scheduleJob: function(event) {
				var id = event.target;
			}
		});
		return XDJobDeployments;
	};
});
