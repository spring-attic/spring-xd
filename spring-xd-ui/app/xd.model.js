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

/*global Backbone, d3, define, clearTimeout, setTimeout, _*/

// This file defines the backbone model used by xd
define([], function() {
	'use strict';

	return function(client, backbone, config) {

		var URL_ROOT = config.urlRoot;
		var ACCEPT_HEADER = config.acceptHeader;
		var PAGE_SIZE = config.pageSize;
		var REFRESH_INTERVAL = config.refreshInterval;

		// The XDModel is a collection of queries known by the client
		// abstract data type for all XD artifacts
		var Artifact = Backbone.Model.extend({});

		// a group of artifacts from the server.
		// has a query associated with it
		var ArtifactGroup = Backbone.Collection.extend({
			XDModel: Artifact
		});

		// to keep track of the pagination retrieved from the server
		var Query = Backbone.Model.extend({
			// expected props: kind, size, totalElements, totalPages, number, artifacts

			getUrl : function() {
				return URL_ROOT + this.get('artifact').url;
			},

			getHttpParams : function() {
				var pageNum = (this.get('number') || 0);
				return { size: PAGE_SIZE, page: pageNum };
			},

			reset : function() {
				this.unset('number');
				this.set('artifacts', []);
			}
		});

		var Model =  Backbone.Model.extend({
			addQuery : function(kind, pageInfo, items) {
				var args = pageInfo || {};
				items = items || [];
				args.artifact = this.findArtifact(kind);
				var artifacts = new ArtifactGroup();
				items.forEach(function(item) {
					artifacts.add(new Artifact(item));
				});
				args.artifacts = artifacts;
				var query = new Query(args);
				this.set(kind, query);
			}
		});

		var XDModel = new Model();

		// define the query kinds we care about
		XDModel.artifacts = [];

		function defineArtifact(kind, name, url) {
			XDModel.artifacts.push({ kind: kind, name: name, url: url });
		}

		function findArtifact(kind) {
			return _.find(XDModel.artifacts, function(artifact) {
				return artifact.kind === kind;
			});
		}

		XDModel.artifacts.forEach(function(artifact) {
			XDModel.addQuery(artifact.kind);
		});

		// Batch Jobs model
		var JobDefinition = Backbone.Model.extend({
			urlRoot: URL_ROOT + '/jobs/',
			url: function() {
				return this.urlRoot + this.id + '/jobs.json';
			}
		});

		var JobDefinitions = Backbone.Collection.extend({
			model: JobDefinition,
			urlRoot: URL_ROOT + "/jobs",
			url: function() {
				return this.urlRoot;
			},
			jobs: [],
			parse: function(response) {
				this.jobs = response.content;
			},
			transformResponse: function(data) {
				for (var i = 0; i < data.length; i++) {
					this.jobs.push({"name": data[i].name });
				}
			},
			startFetching: function() {
				this.fetch({change:true, add:false}).then(
					function() {
						this.fetchTimer = setTimeout(function() {
							if (!this.stopFetch) {
								this.startFetching();
							}
						}.bind(this), REFRESH_INTERVAL);
					}.bind(this));
			},

			stopFetching: function() {
				if (this.fetchTimer) {
					clearTimeout(this.fetchTimer);
				}
				this.stopFetch = true;
			}
		});

		XDModel.jobDefinitions = new JobDefinitions();

		var Execution = Backbone.Model.extend({
			urlRoot: URL_ROOT + 'batch/jobs/',
			url: function() {
				return this.urlRoot + this.id + '.json';
			},
			idAttribute: 'id',
			parse: function(response) {
				return response.jobExecution;
			},
			transform: function() {
				return {
					millis: Math.floor(Math.random() * 1000), // randomized data for now this.get('duration'),
					name: this.id,
					status: this.get('status')
				};
			}

		});
		var Executions = Backbone.Collection.extend({
			model: Execution,
			executions: [],
			urlRoot: URL_ROOT + 'batch/executions',
			url: function() {
				return this.urlRoot;
			},
			parse: function(response) {
				this.executions = response;
			},
			startFetching: function() {
				this.fetch({change:true, add:false}).then(
					function() {
						this.fetchTimer = setTimeout(function() {
							if (!this.stopFetch) {
								this.startFetching();
							}
						}.bind(this), REFRESH_INTERVAL);
					}.bind(this));
			},

			stopFetching: function() {
				if (this.fetchTimer) {
					clearTimeout(this.fetchTimer);
				}
				this.stopFetch = true;
			}
		});

		XDModel.jobExecutions = new Executions();

		var JobInstance = Backbone.Model.extend({
			urlRoot: URL_ROOT + 'batch/jobs/',
			url: function() {
				return this.urlRoot + this.get('name') + '/' + this.id + '.json';
			},
			idAttribute: 'id',
			parse: function(instance) {
				return {
					name: instance.jobName,
					id: instance.id,
					version: instance.version,
					nameId: instance.name+ '/' + instance.id,
					jobParameters: instance.jobParameters,
					jobExecutions: instance.jobExecutions ?
						new Executions(Object.keys(instance.jobExecutions).map(function(key) {
							var execution = new Execution(instance.jobExecutions[key]);
							execution.id = key;
							return execution;
						})) :
						new Executions()
				};
			},
			transformExecutions: function() {
				return this.get('jobExecutions').map(function(execution) {
					return execution.transform();
				});
			}
		});
		var JobInstances = Backbone.Collection.extend({
			jobs: [],
			model: JobInstance,
			urlRoot: URL_ROOT + 'batch/jobs/',
			url: function() {
				return this.urlRoot + this.jobName + '/instances.json';
			},
			parse: function(response) {
				this.jobs = response;
			}
		});

		XDModel.jobInstances = new JobInstances();

		var BatchJob = Backbone.Model.extend({
			urlRoot: URL_ROOT + 'batch/jobs',
			url: function() {
				return this.urlRoot + '/' + this.id + '.json';
			},
			idAttribute: 'name',
			launch: function(parameters) {
				var params = "";
				if (parameters) {
					params = parameters;
				}
				var createPromise = client({
					path: URL_ROOT +  'jobs/' + this.id + '/launch',
					params: { "jobParameters" : params } ,
					method: 'PUT',
					headers: ACCEPT_HEADER
				 });
			return XDModel.batchJobs.fetch({merge:true, update:true });
			},

			parse: function(data) {
				// for some reason coming back as a string
				if (typeof data === 'string') {
					data = JSON.parse(data);
				}
				if (data.job) {
					data = data.job;
				}
				if (data.jobInstances) {
					data.jobInstances = new JobInstances(Object.keys(data.jobInstances).map(function(key) {
						var instance = new JobInstance(data.jobInstances[key]);
						instance.id = key;
						instance.set('name', this.id);
						return instance;
					}, this));
				} else {
					data.jobInstances = new JobInstances();
					data.jobInstances.jobName = data.name;
				}
				return data;
			}
		});

		var BatchJobs = Backbone.Collection.extend({
			model: BatchJob,
			url: URL_ROOT + 'batch/jobs.json',
			jobs: [],
			parse: function(data) {
				this.jobs = data;
			},
			comparator: 'name',

			startFetching: function() {
				this.fetch({change:true, add:false}).then(
					function() {
						this.fetchTimer = setTimeout(function() {
							if (!this.stopFetch) {
								this.startFetching();
							}
						}.bind(this), 5000);
					}.bind(this));
			},

			stopFetching: function() {
				if (this.fetchTimer) {
					clearTimeout(this.fetchTimer);
				}
				this.stopFetch = true;
			}
		});

		XDModel.batchJobs = new BatchJobs();

		return XDModel;
	};
});
