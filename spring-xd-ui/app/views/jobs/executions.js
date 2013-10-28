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
 * View for XD Job Executions
 * @author Ilayaperumal Gopinathan
 */
/*global define, _ */
define([],
function() {
	'use strict';
	return function(Backbone, model, xdJobExecutionsTemplate) {

		var xdJobExecutions = Backbone.View.extend({

			initialize: function(options) {
				this.listenTo(model.jobExecutions, 'change', this.render);
				this.listenTo(model.jobExecutions, 'reset', this.render);
				model.jobExecutions.startFetching();
			},

			template: _.template(xdJobExecutionsTemplate),

			render: function() {
				this.$el.html(this.template({executions: model.jobExecutions.executions}));
				return this;
			}
		});
		return xdJobExecutions;
	};
});
