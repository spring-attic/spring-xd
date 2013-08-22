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
 * view for displaying batch jobs
 */

define(['underscore', 'backbone', 'xd.utils', 'xd.conf', 'xd.model', 'views/batchDetail'],
function(_, Backbone, utils, conf, model, BatchDetail) {

    // maps job names to batchDetails views that are currently expanded
    var expanded = {};

    function extractJob(event) {
        var name = event.currentTarget.getAttribute('job-name');
        return model.batchJobs.get(name);
    }

    var Batch = Backbone.View.extend({
        events: {
            "click button.detailAction": "showDetails",
            "click button.launchAction": "launch"
        },

        initialize: function() {
            this.listenTo(model.batchJobs, 'change', this.render);
            this.listenTo(model.batchJobs, 'reset', this.render);
            model.batchJobs.fetch({change:true, add:false});
        },

        render: function() {
            // first remove all old expanded
            Object.keys(expanded).forEach(function(key) {
                expanded[key].remove();
            }, this);

            this.$el.html(_.template(utils.getTemplate(conf.templates.batchList), { jobs :model.batchJobs.models }));


            // now add the expanded nodes back
            Object.keys(expanded).forEach(function(key) {
                var detailsId = '#' + key + '_details';
                var detailsElt = this.$el.find(detailsId);
                if (detailsElt.length > 0) {
                    detailsElt.replaceWith(expanded[key].$el);
                    detailsElt.collapse('show');
                } else {
                    // job not here any more
                    delete expanded[key];
                }
            }, this);

            return this;
        },

        showDetails: function(event) {
            var job = extractJob(event);
            if (job) {
                var detailsView = expanded[job.id];
                if (detailsView) {
                    // remove from view
                    detailsView.$el.empty();
                    delete expanded[job.id];
                } else {
                    this.stopListening(model.batchJobs, 'change');
                    job.fetch().then(function() {
                        var detailsView = new BatchDetail({job: job });
                        detailsView.setElement('#' + job.id + '_details');
                        detailsView.render();
                        expanded[job.id] = detailsView;
                        this.listenTo(model.batchJobs, 'change', this.render);
                    }.bind(this));
                }
            }
        },

        launch: function(event) {
            var job = extractJob(event);
            if (job) {
                job.launch().then(function() {
                    var detailsView = expanded[job.id];
                    if (detailsView) {
                        job.fetch().then(function() {
                            detailsView.options.job = job;
                            detailsView.render();
                        });
                    }
                });
            }
        }
    });
    return Batch;
});