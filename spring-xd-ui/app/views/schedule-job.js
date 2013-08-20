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

/*
 * View for the dashboard
 */
define(['backbone', 'when', 'xd.utils', 'xd.strings', 'xd.router', 'xd.model'],
function(Backbone, when, utils, strings, router, model) {

    var ScheduleJob = Backbone.View.extend({

        events: {
            'click #schedule-job': 'scheduleJob'
        },

        scheduleJobForm: function() {
            this.$el.html(utils.templateHtml.scheduleJobTemplate);
            this.$('#stream-job').html(utils.templateHtml.jobSelectTemplate);
        },

        resetScheduleJobForm: function() {
            this.$('#stream-name').val('');
            this.scheduleJobForm();
        },

        scheduleJob: function(event) {
            event.preventDefault();
            var jobName = this.$('#job-name').val().trim();
            var jobDefinition = this.$('#job-definition').val().trim();
            var scheduleJob = router.createJob(jobName, jobDefinition, function() {
                router.refresh('jobs');
                utils.showSuccessMsg(strings.scheduleJobSuccess);
            });
            when(scheduleJob).then(this.resetScheduleJobForm());
        }
    });
    return ScheduleJob;
});