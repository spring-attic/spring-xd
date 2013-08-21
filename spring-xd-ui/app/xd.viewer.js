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

define(['xd.utils', 'views/create-stream', 'views/dashboard', 'views/navbar', 'views/schedule-job', 'views/tap-stream', 'views/batch'],
function(utils, CreateStream, Dashboard, Navbar, ScheduleJob, TapStream, Batch) {
    return {
        create : function() {
            var views = {};
            // Initialize the views
            views.navbar = new Navbar({
                el: '#xd-navbar'
            });

            views.dashboard = new Dashboard({
                el: '#xd-dashboard'
            });

            views.createStream = new CreateStream({
                el: '#xd-create-stream'
            });

            views.scheduleJob = new ScheduleJob({
                el: '#xd-schedule-job'
            });

            views.tapStream = new TapStream({
                el: '#xd-tap-stream'
            });

            views.batchList = new Batch({
            });

            //Initialize DOM elements for better handling
            var loading = $('.xd-loading');
            var statusMsg = $('#xd-status-msg');

            $(window.document).on('success', function(evt, message) {
                statusMsg.html('');
                statusMsg.html(utils.updateStatus("Success", message));
            });
            $(window.document).on('error', function(evt, message) {
                statusMsg.html('');
                statusMsg.html(utils.updateStatus("Error", message));
            });

            // Render navbar
            views.navbar.render();

            // Render dashboard
            views.dashboard.render();

            // can't do this earlier since the element is not yet created
            views.batchList.setElement($('#xd-batch-list')[0]);

            views.done = function() {
                loading.hide();
            };

            return views;
        }
    };
});