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
/*global d3 cubism */

/*
 * View for the dashboard
 */

define(['underscore', 'when', 'backbone', 'xd.utils', 'xd.conf', 'xd.model', 'xd.router', 'views/gauge', 'xd.strings', 'cubism', 'd3'],
function(_, when, Backbone, utils, conf, model, router, gauge, strings) {

    // keep track of expanded state
    var expanded = {};
    // TODO only works on document...why?
    $(document).on('show.bs.collapse', function (evt) {
        expanded[evt.target.id] = true;
    });
    $(document).on('hide.bs.collapse', function (evt) {
        expanded[evt.target.id] = false;
    });

    function createEvents(dasboard) {
        var events = {
            "click button.deleteAction": "deleteArtifact",
            "click button.detailAction": "toggleDetails"
        };
        model.artifactKinds.forEach(function(kind) {
            events['click #xd-' + kind + '-list .pagination #pagelinks li'] = 'set' + kind + 'Page';
        });
        return events;
    }

    var Dashboard = Backbone.View.extend({

        events: createEvents(),

        initialize: function() {
            model.artifactKinds.forEach(function(kind) {
                this['set' + kind + 'Page'] = function(event) {
                    this.setPage(event, kind);
                };
                this['load' + kind + 'List'] = function(event) {
                    this.loadList(event, kind);
                };
            }, this);
        },

        render: function(data) {
            this.$el.html(_.template(utils.getTemplate(conf.templates.dashboard), {
                kindNames: model.readableNames
            }));
        },

        // sets the current page of model element and loads it
        setPage : function(event, kind) {
            if (typeof event !== 'undefined') {
                event.preventDefault();
                var newPageAttr = event.currentTarget.attributes.pageNumber;
                if (newPageAttr) {
                    model.get(kind).set('number', parseInt(newPageAttr.value, 10));
                }
            }
            router.refresh(kind);
        },

        loadList: function(kind, event) {
            if (typeof event !== 'undefined') {
                event.preventDefault();
            }

            var templateVars;
            var query = model.get(kind);
            var artifacts = query.get('artifacts');
            var pageNumber = query.get('number') || 1;
            var artifactsTable = $('#' + kind + '-table > table');
            if (artifactsTable.length === 0) {
                var listHtml = _.template(utils.getTemplate(conf.templates.artifactsList), { artifact_kind : kind });
                artifactsTable = $('#' + kind + '-table').html(listHtml);
            }
            var artifactsBody = artifactsTable.find('tbody');
            // first remove all missing elements
            artifactsBody.find('tr').each(function(i, row) {
                // remove the rows with id foo and with id foo_detailsRow
                var name = row.id;
                if (name.indexOf('_detailsRow') == name.length - '_detailsRow'.length) {
                    name = name.substring(0, name.length - '_detailsRow'.length);
                }
                if (artifacts.where({ name: name }).length === 0) {
                    $(row).detach();
                }
            });

            // now add all elements that didn't exist before
            var prevRow;
            artifacts.each(function(artifact) {
                var name = artifact.get('name');
                if (artifactsBody.find('#' + name).length === 0) {
                    // row doesn't exist, insert it
                    var templateArgs = {
                        name: name,
                        status: 'deployed', // need to get this one for real later
                        definition: artifact.get('definition'),
                        isExpanded: expanded[name]
                    };
                    var rawHtml = _.template(utils.getTemplate(conf.templates.artifactsListItem), templateArgs);
                    if (prevRow) {
                         prevRow.after(rawHtml);
                    } else {
                        // insert as first element
                        artifactsBody.prepend(rawHtml);
                    }
                }
                prevRow = artifactsBody.find('#' + name + '_detailsRow');
            });

            // create the pagination
            templateVars = {};
            templateVars.pages = query.get('totalPages');
            templateVars.current_page = query.get('number');
            var paginationHtml = _.template(utils.getTemplate(conf.templates.pagination), templateVars);
            this.$('#' + kind + '-pagination').html(paginationHtml);
        },
        
        toggleDetails: function(event) {},
        toggleDetailsBAK: function(event) {
            // TODO brittle
            var name = $(event.target).parents('tr').attr('id');
            var show = !$(event.target).parents('table').find('#' + name + '_details').hasClass('in');
            var analyticsId = '#' + name + '_analytics';
            var gaugeId = '#' + name + '_gauge';
            var kind = $(event.target).parents('table').attr('id');
            if (show) {

                // create the time series chart
                var context = cubism.context()
                    .step(10000)
                    .size(800)
                    .start();

                var analytics = model.addAnalytics(kind, name, context);
                var metric = analytics.get('metric');
                d3.select(analyticsId).selectAll(".axis")
                    .data(["bottom"])
                    .enter().append("div")
                    .attr("class", function(d) { return d + " axis"; })
                    .each(function(d) {
                        d3.select(this).call(context.axis().ticks(12).orient(d));
                    });

                d3.select(analyticsId).append("div")
                    .attr("class", "rule")
                    .call(context.rule());

                d3.select(analyticsId).selectAll(".horizon")
                    .data([metric])
                    .enter().insert("div", ".bottom")
                    .attr("class", "horizon")
                    .call(context.horizon().height(100)
                    .format(d3.format(" $,.0f")));

                // uncomment if we want rhs number to move with mouse
//                context.on("focus", function(i) {
//                  d3.selectAll(".value").style("right", i == null ? null : context.size() - i + "px");
//                });

                // create the gauge
                var randomGauge = gauge(gaugeId, {
                    maxValue: 1000,
                    clipWidth: 250
                });
                randomGauge.render();
                analytics.on('change:values', function() {
                    var values = this.get('values');
                    randomGauge.update(values[values.length-1]);
                });

            } else {
                $(analyticsId).empty();
                $(gaugeId).empty();
                model.removeAnalytics(kind, name);
            }

        },

        performAction: function(event, action) {
            event.preventDefault();
            // TODO brittle
            var name = $(event.target).parents('tr').attr('id');
            var kind = $(event.target).parents('table').attr('id');
            if (name) {
                router.performAction(name, action, kind, function() {
                    utils.showSuccessMsg(strings.action[action]);
                    router.refresh(kind);
                });
            }
        },

        deleteArtifact: function(event) {
            event.preventDefault();
            this.performAction(event, "delete");
        }
    });

    return Dashboard;
});