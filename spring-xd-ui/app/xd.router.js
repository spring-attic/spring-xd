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

define(['underscore', 'jquery', 'when', 'rest', 'rest/interceptor/entity', 'rest/interceptor/mime', 'rest/interceptor/hateoas', 'rest/interceptor/errorcode', 'backbone', 'xd.conf', 'xd.utils', 'xd.strings',
    'xd.model'],
function(_, $, when, rest, entity, mime, hateoas, errorcode, Backbone, conf, utils, strings, model) {
    'use strict';

    var resourceLinks = {}, views;

    // set up the rest client
    var ACCEPT_HEADER = { 'Accept': 'application/json' };
    var URL_ROOT = 'http://localhost:8080/';
    var client = rest.chain(errorcode, { code: 400 }).chain(mime).chain(hateoas).chain(entity);

    var ajax = {};
    /*
     * Backbone Router object for xd app
     */
    ajax.xdRouter = Backbone.Router.extend({

        routes: {
            "dashboard/:artifact/:search":         "showDashboardPage",
            "createStream":     "showCreateStream",
            "createTap":     "showCreateTap",
            "createJob":     "showCreateJob"
        },

        initialize: function(viewer) {
            this.setUp(viewer);
        },
        setUp: function(viewer) {

            views = viewer.create();

            var refreshModel = function(queryKind) {
                var query = model.get(queryKind);
                return client({
                    path: query.getUrl(),
                    params: query.getHttpParams(),
                    headers: ACCEPT_HEADER
                }).then(
                function(items) {
                    model.addQuery(queryKind, items.page, items.content);
                },
                function(error) {
                    utils.showError(error);
                });
            };

            // define the functions to refresh artifacts
            model.artifactKinds.forEach(function(kind) {
                ajax[kind] = function() { return refreshModel(kind); };
            });

            // only delete supported right now
            ajax.performAction = function(name, action, kind, successCallback) {
                var method = "PUT";
                if (action === "delete") {
                    method = "DELETE";
                }
                var query = model.get(kind);
                return client({
                    path: query.getUrl() + '/' + name,
                    headers: ACCEPT_HEADER,
                    method: method
                }).then(successCallback, function(error) {
                    utils.showErrorMsg(error);
                });
            };

            // can only create streams, taps, and jobs. Others are created implicitly from a tap or job
            ajax.createArtifact = function(kind, name, definition, successCallback) {
                var query = model.get(kind);
                return client({
                    path: query.getUrl(),
                    params: { name : name, definition: definition },
                    headers: ACCEPT_HEADER,
                    method: 'POST'
                }).then(successCallback, function(error) {
                    utils.showErrorMsg(error);
                });
            };

            ajax.createStream = function(streamName, definition, successCallback) {
                if (!definition) {
                    definition = 'time|log';
                }
                return ajax.createArtifact('streams', streamName, definition, successCallback);
            };

            ajax.createJob = function(jobName, definition, successCallback) {
                return ajax.createArtifact('jobs', jobName, definition, successCallback);
            };

            ajax.createTap = function(tapName, targetStream, definition, successCallback) {
                if (!definition) {
                    definition = 'log';
                }
                if (definition.charAt(0) !== '|') {
                    definition = '|' + definition;
                }
                // TODO this is the old syntax. Fix to make it new
                definition = 'tap@' + targetStream + ' ' + definition;

                return ajax.createArtifact('taps', tapName, definition, successCallback);
            };

            ajax.refresh = function(kind) {
                this[kind]().then(function() {
                    views.dashboard.loadList(kind);
                });
            };



            // Refresh all artifacts now and over time
            model.artifactKinds.forEach(function(kind) {
                ajax.refresh(kind);
                setInterval(function() {
                    ajax.refresh(kind);
                }.bind(this), conf.refreshInterval);
            }, this);

            // now initialoize the dashboard and views
            // Render artifact creation forms
            views.createStream.defaultStreamForm();
            views.tapStream.tapStreamForm();
            views.scheduleJob.scheduleJobForm();
            views.done();
        },

        refreshStatus: function(kind) {
            var query = model.get(kind);
            var artifacts = query.get('artifacts');
            if (artifacts.isEmpty()) { return; }
            artifacts.forEach(function(artifact) {
               return client({
                    path: query.getUrl() + '/' + artifact.get('name'),
                    headers: ACCEPT_HEADER
                }).then(function(data) {
                    // this is not really right since we need to actually find if deployed or not
                    artifact.set('status', 'deployed');
                },
                function(error) {
                    utils.showError(error);
                });
            });
        }
    });

    return ajax;
});