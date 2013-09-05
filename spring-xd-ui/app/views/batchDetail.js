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
 * @author Andrew Eisenberg
 */

/*
 * View for displaying details of a batch job
 */

define(['underscore', 'backbone', 'xd.utils', 'xd.conf', 'xd.model', 'views/bar'],
function(_, Backbone, utils, conf, model, bar) {

    var BatchDetails = Backbone.View.extend({
        events: {
            // careful here...it might be that multiple views capture the same event
            'click .jobInstance' : 'showExecutions'
        },

        render: function() {
            this.$el.html(_.template(utils.getTemplate(conf.templates.batchDetails), this.options.job.attributes));
            if (this.currentExecutionId) {
                this.showExecutions(this.currentExecutionId);
            }
            this.delegateEvents(this.events);
            return this;
        },

        showExecutions : function(event) {
            // de-select all rows
            var selector = '#' + this.options.job.id + '-executions';
            var id = event.currentTarget ? event.currentTarget.getAttribute('instanceId') : event;
            var jobInstance = this.options.job.attributes.jobInstances.get(id);
            if (jobInstance) {
                jobInstance.fetch({ merge:true, update:true }).then(function() {
                    $(selector).empty();
                    var graph = bar(jobInstance.transformExecutions(), selector);
                    this.currentExecutionId = id;

                    // select this row
                    this.$('table.info-table tr').removeClass('info');
                    this.$('tr#' + this.options.job.id + '-instance-' + id).addClass('info');
                }.bind(this));
            }
        },

        destroy: function() {
            // calling this.remove() will remove the dom node from the tree, which is not what we want
            var selector = '#' + this.options.job.id + '-executions';
            $(selector).empty();
            this.$el.empty();
            this.$el.unbind();
            this.$detailsRow.hide();
            this.stopListening();

        }
    });

    return BatchDetails;
});