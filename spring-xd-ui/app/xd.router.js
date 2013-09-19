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


/*jshint browser:true */
/*global define */

define([], function() {
    'use strict';
    var routerFactory = function(when, Backbone, utils, model, client, REFRESH_INTERVAL) {

        /*
         * Backbone Router object for xd app
         */
        var XDRouter = Backbone.Router.extend({

            // TODO these routes are not used yet
            routes: {
                'dashboard/:artifact/:search':         'showDashboardPage',
                'createStream':     'showCreateStream',
                'createTap':     'showCreateTap',
                'createJob':     'showCreateJob'
            },

            // called by viewer
            initializeViewRouting: function(views) {

                var refreshModel = function(queryKind) {
                    var query = model.get(queryKind);
                    return client({
                        path: query.getUrl(),
                        params: query.getHttpParams()
                    }).then(
                    function(items) {
                        model.addQuery(queryKind, items.page, items.content);
                    },
                    function(error) {
                        utils.showError(error);
                    });
                };

                // define the functions to refresh artifacts
                model.artifacts.forEach(function(artifact) {
                    this[artifact.kind] = function() { return refreshModel(artifact.kind); };
                }.bind(this));

                // only delete supported right now
                this.performAction = function(name, action, kind, successCallback) {
                    var method = 'PUT';
                    if (action === 'delete') {
                        method = 'DELETE';
                    }
                    var query = model.get(kind);
                    return client({
                        path: query.getUrl() + '/' + name,
                        method: method
                    }).then(successCallback, function(error) {
                        utils.showErrorMsg(error);
                    });
                };

                // can only create streams, taps, and jobs. Others are created implicitly from a tap or job
                this.createArtifact = function(kind, name, definition, successCallback) {
                    var query = model.get(kind);
                    return client({
                        path: query.getUrl(),
                        params: { name : name, definition: definition },
                        method: 'POST'
                    }).then(successCallback, function(error) {
                        utils.showErrorMsg(error);
                    });
                };

                this.createStream = function(streamName, definition, successCallback) {
                    if (!definition) {
                        definition = 'time|log';
                    }
                    return this.createArtifact('streams', streamName, definition, successCallback);
                };

                this.createJob = function(jobName, definition, successCallback) {
                    return this.createArtifact('jobs', jobName, definition, successCallback);
                };

                this.createTap = function(tapName, targetStream, definition, successCallback) {
                    if (!definition) {
                        definition = 'log';
                    }
                    if (definition.charAt(0) !== '|') {
                        definition = '|' + definition;
                    }
                    // TODO this is the old syntax. Fix to make it new
                    definition = 'tap@' + targetStream + ' ' + definition;

                    return this.createArtifact('taps', tapName, definition, successCallback);
                };

                this.refresh = function(kind) {
                    this[kind]().then(function() {
                        views.dashboard.loadList(kind);
                    }.bind(this));
                }.bind(this);



                // Refresh all artifacts now and over time
                model.artifacts.forEach(function(artifact) {
                    this.refresh(artifact.kind);
                    setInterval(function() {
                        this.refresh(artifact.kind);
                    }.bind(this), REFRESH_INTERVAL);
                }, this);

                views.done();
            },

            refreshStatus: function(kind) {
                var query = model.get(kind);
                var artifacts = query.get('artifacts');
                if (artifacts.isEmpty()) { return; }
                artifacts.forEach(function(artifact) {
                   return client({
                        path: query.getUrl() + '/' + artifact.get('name')
                    }).then(function() {
                        // this is not really right since we need to actually find if deployed or not
                        artifact.set('status', 'deployed');
                    },
                    function(error) {
                        utils.showError(error);
                    });
                });
            }
        });
        return new XDRouter();
    };
    return routerFactory;
});
