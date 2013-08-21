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

define(['underscore', 'backbone', 'xd.utils', 'xd.conf', 'xd.model'],
function(_, Backbone, utils, conf, model) {

    var BatchDetails = Backbone.View.extend({
        events: { },

        initialize: function() {

        },

        render: function() {
            this.$el.html(_.template(utils.getTemplate(conf.templates.batchDetails), this.options.details));
            return this;
        }
    });

    return BatchDetails;
});