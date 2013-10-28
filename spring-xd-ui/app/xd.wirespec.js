
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
 * @author Andrew Eisenberg
 * @author Ilayaperumal Gopinathan
 */

// Wire spec for the admin ui

/*global define */

define({

	config: {
		module: 'xd.config'
	},
	// Backbone model
	model: {
		create: {
			module: 'xd.model',
			args: [
				{ $ref: 'client' },
				{ $ref: 'backbone' },
				{ $ref: 'config' }
			]
		}
	},
	// Backbone view
	viewer: {
		create: {
			module: 'xd.viewer',
			args: [
				{ $ref: 'utils' },
				{ $ref: 'jobDefinitions' },
				{ $ref: 'jobDeployments' },
				{ $ref: 'jobExecutions' },
				{ $ref: 'router' }
			]
		},
		properties: {
			jobDefinitionsElt: { $ref: 'dom!job-definitions'},
			jobDeploymentsElt: { $ref: 'dom!job-deployments'},
			jobExecutionsElt: { $ref: 'dom!job-executions'},
			xdLoadingElt: { $ref: 'dom!xd-loading'},
			xdStatusMsgElt: { $ref: 'dom!xd-status-msg'}
		},
		init: 'init'
	},
	// Backbone router
	router: {
		create: {
			module: 'xd.router',
			args: [
				{ $ref: 'when' },
				{ $ref: 'backbone' },
				{ $ref: 'utils' },
				{ $ref: 'model' },
				{ $ref: 'client' },
				{ $ref: 'config'}
			]
		}
	},
	client: {
		create: {
			module: 'xd.client',
			args: [
				{ $ref: 'rest' },
				{ $ref: 'entity' },
				{ $ref: 'mime' },
				{ $ref: 'hateoas' },
				{ $ref: 'errorCode' },
				{ $ref: 'config' }
			]
		}
	},
	utils: {
		create: {
			module: 'xd.utils',
			args: [
				{ $ref: 'statusMsgTemplate' }
			]
		}
	},
	strings: {
		module: 'xd.strings'
	},

	// views
	jobDefinitions: {
		create: {
			module: 'views/jobs/definitions',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'model' },
				{ $ref: 'xdJobDefinitionsTemplate' }
			]
		}
	},

	jobDeployments: {
		create: {
			module: 'views/jobs/deployments',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'model' },
				{ $ref: 'xdJobDeploymentsTemplate' }
			]
		}
	},

	jobExecutions: {
		create: {
			module: 'views/jobs/executions',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'model' },
				{ $ref: 'xdJobExecutionsTemplate' }
			]
		}
	},

	// templates
	statusMsgTemplate: { module: 'text!../templates/container/status-msg.tpl' },
	xdJobDefinitionsTemplate: { module: 'text!../templates/jobs/definitions.tpl' },
	xdJobDeploymentsTemplate: { module: 'text!../templates/jobs/deployments.tpl' },
	xdJobExecutionsTemplate: { module: 'text!../templates/jobs/executions.tpl' },
	//batchListTemplate: { module: 'text!../templates/batch/batch-list.tpl' },
	//batchDetailsTemplate: { module: 'text!../templates/batch/batch-details.tpl' },
	//navbarTemplate: { module: 'text!../templates/container/navbar.tpl' },
	//dashboardTemplate: { module: 'text!../templates/dashboard/dashboard.tpl' },
	//paginationTemplate: { module: 'text!../templates/dashboard/pagination.tpl' },
	//artifactsListTemplate: { module: 'text!../templates/dashboard/artifacts-list.tpl' },
	//artifactsListItemTemplate: { module: 'text!../templates/dashboard/artifacts-list-item.tpl' },
	//createDefaultStreamTemplate: { module: 'text!../templates/stream/default-stream.tpl' },
	//tapStreamTemplate: { module: 'text!../templates/stream/tap-stream.tpl' },
	//createJobTemplate: { module: 'text!../templates/jobs/create-job.tpl' },

	// d3 graphical components
	// not wired yet
	// bar: {

	// },

	// guage: {

	// },


	// third party libraries
	backbone: {
		module: 'backbone'
	},

	// just load d3 as a global
	d3: {
		module: 'd3'
	},

	// rest library
	when: { module: 'when'},
	rest: { module: 'rest'},
	entity: { module: 'rest/interceptor/entity'},
	mime: { module: 'rest/interceptor/mime'},
	hateoas: { module: 'rest/interceptor/hateoas'},
	errorCode: { module: 'rest/interceptor/errorCode'},

	plugins : [
			{ module : 'wire/jquery/dom' }
	]
});
