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
 */

// Wire spec for the admin ui

/*global define */

define({
	// TODO should be configurable
	URL_ROOT: 'http://localhost:9393/',

	// TODO should be configurable
	PAGE_SIZE: 5,

	ACCEPT_HEADER: { 'Accept': 'application/json' },

	// for polling server, how long do we wait?
	REFRESH_INTERVAL: 4000,

	model: {
		create: {
			module: 'xd.model',
			args: [
				{ $ref: 'client' },
				{ $ref: 'backbone' },
				{ $ref: 'URL_ROOT' },
				{ $ref: 'PAGE_SIZE' },
				{ $ref: 'ACCEPT_HEADER' }
			]
		}
	},
	viewer: {
		create: {
			module: 'xd.viewer',
			args: [
				{ $ref: 'utils' },
				{ $ref: 'createStream' },
				{ $ref: 'dashboard' },
				{ $ref: 'navbar' },
				{ $ref: 'createJob' },
				{ $ref: 'tapStream' },
				{ $ref: 'batch' },
				{ $ref: 'router' }
			]
		},
		properties: {
			navbarElt: { $ref: 'dom!xd-navbar'},
			dashboardElt: { $ref: 'dom!xd-dashboard'},
			createStreamElt: { $ref: 'dom!xd-create-stream'},
			createJobElt: { $ref: 'dom!xd-create-job'},
			createTapElt: { $ref: 'dom!xd-create-tap'},
			xdLoadingElt: { $ref: 'dom!xd-loading'},
			xdStatusMsgElt: { $ref: 'dom!xd-status-msg'}

		},
		init: 'init'
	},
	router: {
		create: {
			module: 'xd.router',
			args: [
				{ $ref: 'when' },
				{ $ref: 'backbone' },
				{ $ref: 'utils' },
				{ $ref: 'model' },
				{ $ref: 'client' },
				{ $ref: 'REFRESH_INTERVAL'}
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
				{ $ref: 'ACCEPT_HEADER' }
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
		module: 'xd.strings',
	},

	// views
	dashboard: {
		create: {
			module: 'views/dashboard',
			args: [
				{ $ref: 'when' },
				{ $ref: 'backbone' },
				{ $ref: 'utils' },
				{ $ref: 'model' },
				{ $ref: 'router' },
				{ $ref: 'strings' },
				{ $ref: 'artifactsListTemplate' },
				{ $ref: 'artifactsListItemTemplate' },
				{ $ref: 'dashboardTemplate' },
				{ $ref: 'paginationTemplate' }
			]
		}
	},
	batch: {
		create: {
			module: 'views/batch',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'model' },
				{ $ref: 'batchDetail' },
				{ $ref: 'batchListTemplate'}
			]
		}
	},
	batchDetail: {
		create: {
			module: 'views/batchDetail',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'model' },
				{ $ref: 'batchDetailsTemplate'}
			]
		}
	},
	createStream: {
		create: {
			module: 'views/create-stream',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'when' },
				{ $ref: 'utils' },
				{ $ref: 'router' },
				{ $ref: 'strings' },
				{ $ref: 'createDefaultStreamTemplate' }
			]
		}
	},
	createJob: {
		create: {
			module: 'views/create-job',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'when' },
				{ $ref: 'utils' },
				{ $ref: 'strings' },
				{ $ref: 'router' },
				{ $ref: 'createJobTemplate' }
			]
		}
	},
	tapStream: {
		create: {
			module: 'views/tap-stream',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'when' },
				{ $ref: 'utils' },
				{ $ref: 'router' },
				{ $ref: 'strings' },
				{ $ref: 'model' },
				{ $ref: 'tapStreamTemplate' }
			]
		}
	},
	navbar: {
		create: {
			module: 'views/navbar',
			args: [
				{ $ref: 'backbone' },
				{ $ref: 'navbarTemplate' }
			]
		}
	},

	// templates
	batchListTemplate: { module: 'text!../templates/batch/batch-list.tpl' },
	batchDetailsTemplate: { module: 'text!../templates/batch/batch-details.tpl' },
	statusMsgTemplate: { module: 'text!../templates/container/status-msg.tpl' },
	navbarTemplate: { module: 'text!../templates/container/navbar.tpl' },
	dashboardTemplate: { module: 'text!../templates/dashboard/dashboard.tpl' },
	paginationTemplate: { module: 'text!../templates/dashboard/pagination.tpl' },
	artifactsListTemplate: { module: 'text!../templates/dashboard/artifacts-list.tpl' },
	artifactsListItemTemplate: { module: 'text!../templates/dashboard/artifacts-list-item.tpl' },
	createDefaultStreamTemplate: { module: 'text!../templates/stream/default-stream.tpl' },
	tapStreamTemplate: { module: 'text!../templates/stream/tap-stream.tpl' },
	createJobTemplate: { module: 'text!../templates/job/create-job.tpl' },

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
			{ module : 'wire/debug' },
			{ module : 'wire/jquery/dom' }
	]
});
