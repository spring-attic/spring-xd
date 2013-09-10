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

    // accesses data for the bar graph
    var dataAccessor = {
        // accessor functions
        // TODO allow these to be passed in as options
        barLabel: function(d) {
            return d.id;
        },
        barValue: function(d) {
            return d.attributes.endTime - d.attributes.startTime;
        },
        statusColor: function(d) {
            switch (this.statusText(d)) {
                case 'FAILED':
                    return '#f2dede';  // bootstrap alert-error color
                case 'COMPLETED':
                case 'SUCCESS':
                    return '#dff0d8';  // bootstrap alert-success color
                case 'STARTED':
                default:
                  return '#ffffff';
              }
        },
        statusText: function(d) {
            return d.attributes.exitStatus.exitCode;
        },
        jobParameters: function(d) {
            try {
              return JSON.stringify(d.attributes.jobParameters.parameters);
            } catch (e) {
              return '<em>no parameters</em>';
            }
        },
        barHover: function(d) {
            return '<span style="font-size:16px">Execution ' + this.barLabel(d) + 
                ': <span style="font-weight:bold; color:' + this.statusColor(d) + '">' +
                this.statusText(d) + '</span> Duration ' + this.barValue(d) + 
                'ms<br/>Job Parameters:' + this.jobParameters(d) + '</span>';
        }
    };

    var BatchDetails = Backbone.View.extend({
        events: {
            // careful here...it might be that multiple views capture the same event
            // 'click .jobInstance' : 'showExecutions'
        },

        render: function() {
            this.$el.html(_.template(utils.getTemplate(conf.templates.batchDetails), this.options.job.attributes));
            this.showExecutions(this.currentExecutionId);
            this.delegateEvents(this.events);
            return this;
        },

        showExecutions : function(event) {
            // de-select all rows
            var id = this.options.job.id;
            var selector = '#' + id + '-executions';
            var jobExecutions = new model.Executions();
            jobExecutions.jobName = id;
            jobExecutions.fetch().then(function() {
                $(selector).empty();
                var activeTargets = this.findActiveTipsys();
                var graph = bar(jobExecutions.models, dataAccessor, selector, this.$);
                this.reattachTipsy(activeTargets);
                this.currentExecutionId = id;
            }.bind(this));
        },

        // not used, but could be used to show executions for a given instance
        showInstanceExecutions : function(event) {
            // de-select all rows
            var selector = '#' + this.options.job.id + '-executions';
            var id = event.currentTarget ? event.currentTarget.getAttribute('instanceId') : event;
            var jobInstance = this.options.job.attributes.jobInstances.get(id);
            if (jobInstance) {
                jobInstance.fetch({ merge:true, update:true }).then(function() {
                    $(selector).empty();
                    var graph = bar(jobInstance.transformExecutions(), selector, this.$);
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

        },

        /**
         * finds all executions with active tipsy hovers
         */
        findActiveTipsys: function() {
            return $('svg rect').map(function(i, elt) {
                var tips = $(elt).tipsy(true);
                return tips && tips.hoverState === 'in' && elt.getAttribute('label');
            });
        },

        // this should go into bar.js
        reattachTipsy: function(activeTargets) {
            var rects = this.$('svg rect');

            // close all old
            rects.tipsy.revalidate();

            // reattach new events
            rects.tipsy({
                gravity: 'e',
                html: true,
                // fade: true,
                // delayIn: 150,
                // delayOut: 150,
                title: function() {
                    return dataAccessor.barHover(this.__data__);
                }
            });

            // finally, reshow the previously hiddlen tipsys
            activeTargets.each(function(i, label) {
                if (label) {
                    rects.each(function(i, elt ) {
                        if (elt.getAttribute('label') === label) {
                            $(elt).tipsy('show');
                        }
                    });
                }
            });
        }
    });

    return BatchDetails;
});