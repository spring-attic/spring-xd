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
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */

/*jslint browser:true */
/*global d3 define _ */

// This file defines the backbone model used by xd
define(['backbone', 'rest', 'rest/interceptor/entity', 'rest/interceptor/mime', 'rest/interceptor/hateoas', 'rest/interceptor/errorCode', 'd3'],
function(Backbone, rest, entity, mime, hateoas, errorcode) {

    // set up the rest client
    // this is a copy from router
    var ACCEPT_HEADER = { 'Accept': 'application/json' };
    var URL_ROOT = 'http://localhost:8080/';
    var client = rest.chain(errorcode, { code: 400 }).chain(mime).chain(hateoas).chain(entity);


	// page size is hardcoded, but should be configurable
    var PAGE_SIZE = 5;

    // The model is a collection of queries known by the client
    // abstract data type for all XD artifacts
    var Artifact = Backbone.Model.extend({});

    // a group of artifacts from the server.
    // has a query associtted with it
    var ArtifactGroup = Backbone.Collection.extend({
        model: Artifact
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

    // analytics for an xd artifact
    var Analytics = Backbone.Model.extend({});

    var AllAnalytics = Backbone.Collection.extend({
        model: Analytics
    });
    var allAnalytics = new AllAnalytics();

    var Model =  Backbone.Model.extend({
        addQuery : function(kind, pageInfo, items) {
            var args = pageInfo || {};
            items = items || [];
            args.artifact = model.findArtifact(kind);
            var artifacts = new ArtifactGroup();
            items.forEach(function(item) {
                artifacts.add(new Artifact(item));
            });
            args.artifacts = artifacts;
            var query = new Query(args);
            this.set(kind, query);
        },

        addAnalytics : function(kind, name, cubismContext) {
            var analytics = new Analytics({kind:kind, name:name});
            allAnalytics.add(analytics);
            var format = d3.time.format("%d-%b-%y");
            var metric = cubismContext.metric(function(start, stop, step, callback) {
                d3.json("data/" + name, function(values) {
                    analytics.set('values', values);
                    callback(null, values.slice(-cubismContext.size()));
                });
            }, name);
            analytics.set('metric', metric);
            return analytics;
        },

        removeAnalytics : function(kind, name) {
            allAnalytics.remove(allAnalytics.where({kind:kind, name:name}));
        }
    });
    var model = new Model();

    // define the query kinds we care about
    model.artifacts = [];
    function defineArtifact(kind, name, url) {
		model.artifacts.push({ kind: kind, name: name, url: url });
    }
    model.findArtifact = function(kind) {
		return _.find(model.artifacts, function(artifact) {
			return artifact.kind === kind;
		});
    };
//    defineArtifact('streams', 'Streams', 'streams');
    defineArtifact('jobs', 'Jobs', 'jobs');
//    defineArtifact('counters', 'Counters', 'metrics/counters');
//    defineArtifact('field-value-counters', 'Field Value Counters', 'metrics/field-value-counters');
//    defineArtifact('aggregate-counters', 'Aggregate Counters', 'metrics/aggregate-counters');

//    these artifacts no longer exist
//    defineArtifact('taps', 'Taps', 'taps');
//    defineArtifact('triggers', 'Triggers', 'triggers');
//    defineArtifact('guages', 'Guages', 'metrics/guages');
//    defineArtifact('richguages', 'Rich Guages', 'metrics/richguages');
	


    model.artifacts.forEach(function(artifact) {
        model.addQuery(artifact.kind);
    });

    model.set('allAnalytics', allAnalytics);



    /////////////////////////////////////////////////////////////////////////////////////
    // stuff for batch.
    // Here is the model:
    //batch jobs
    //batch job
    //instances
    //instance
    //executions
    //execution
    //stepExecutions
    //stepExecution
    /////////////////////////////////////////////////////////////////////////////////////

    // stuff for batch
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
            
            return model.batchJobs.fetch({merge:true, update:true });
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

        parse: function(data) {
            return data;
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

    var Execution = Backbone.Model.extend({
        urlRoot: URL_ROOT + 'batch/jobs/',
        url: function() {
            return this.urlRoot + this.id + '/executions.json';
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
        urlRoot: URL_ROOT + 'batch/jobs/',
        url: function() { 
            return this.urlRoot + this.jobName + '/executions/';
        }
    });

    // Not used at the moment
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
        model: JobInstance,
        urlRoot: URL_ROOT + 'batch/jobs/',
        url: function() {
            return this.urlRoot + this.jobName + '/instances.json';
        }
    });



    model.batchJobs = new BatchJobs();
    model.Executions = Executions;
    return model;
});
