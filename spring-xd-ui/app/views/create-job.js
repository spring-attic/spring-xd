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

 /*global define */

/*
 * View for the dashboard
 */
define([],
function() {
    'use strict';
    return function(Backbone, when, utils, strings, router, createJobTemplate) {
        var CreateJob = Backbone.View.extend({

            events: {
                'click #create-job': 'createJob'
            },

            render: function() {
                this.$el.html( _.template(createJobTemplate));
                return this;
            },

            resetCreateJobForm: function() {
                this.$('#stream-name').val('');
                this.render();
            },

            createJob: function(event) {
                event.preventDefault();
                var jobName = this.$('#job-name').val().trim();
                var jobDefinition = this.$('#job-definition').val().trim();
                var createJob = router.createJob(jobName, jobDefinition, function() {
    //                router.refresh('jobs');
                    utils.showSuccessMsg(strings.createJobSuccess);
                });
                when(createJob).then(this.resetCreateJobForm());
            }
        });
        return CreateJob;
    };
});