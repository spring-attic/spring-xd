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

// TODO move all these module parameters into the anonymous function
define([], function() {
    return function(utils, CreateStream, Dashboard, Navbar, CreateJob, TapStream, BatchList, router) {
        var views = {};

        // The wire.js init function
        // called after initialization
        // important dom nodes have been injected
        views.init = function() {
            // Initialize the views
            views.navbar = new Navbar({
                el: this.navbarElt
            });

            views.dashboard = new Dashboard({
                el: this.dashboardElt
            });

            views.createStream = new CreateStream({
                el: this.createStreamElt
            });

            views.createJob = new CreateJob({
                el: this.createJobElt
            });

            views.tapStream = new TapStream({
                el: this.createTapElt
            });

            views.batchList = new BatchList();

            //Initialize DOM elements for better handling
            var loading = $(this.xdLoadingElt);
            var statusMsg = $(this.xdStatusMsgElt);

            $(window.document).on('success', function(evt, message) {
                statusMsg.html('');
                statusMsg.html(utils.updateStatus("Success", message));
            });
            $(window.document).on('error', function(evt, message) {
                statusMsg.html('');
                statusMsg.html(utils.updateStatus("Error", message));
            });

            views.navbar.render();
            views.dashboard.render();
            views.createJob.render();
            views.createStream.render();
            views.tapStream.render();

            // can't do this earlier since the element is not yet created
            views.batchList.setElement($('#xd-batch-list')[0]);

            views.done = function() {
                loading.hide();
            };

            // hook the views up to router functions
            router.initializeViewRouting(views);
        };

        return views;
    };


});