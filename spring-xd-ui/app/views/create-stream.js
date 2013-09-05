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


/*
 * View for the dashboard
 */

define(['underscore', 'backbone', 'when', 'xd.utils', 'xd.conf', 'xd.router', 'xd.strings'],
function(_, Backbone, when, utils, conf, router, strings) {
    var CreateStream = Backbone.View.extend({
        events: {
            'click #create-msgstream-form #custom-options': 'customStreamForm',
            'click #create-msgstream-form #default-options': 'defaultStreamForm',
            'click #create-default-stream': 'createDefaultStream',
            'change #source-selector': 'selectSourceParams',
            'change #processor-selector': 'selectProcessorParams',
            'change #sink-selector': 'selectSinkParams'
        },

        defaultStreamForm: function(event) {
            if (event !== null && typeof event !== 'undefined') {
                event.preventDefault();
            }
            var streamName = this.$('#stream-name').val();
            this.$el.html(utils.templateHtml.defaultStreamTemplate);
            if (typeof streamName !== 'undefined') {
                this.$('#create-msgstream-form #stream-name').attr('value', streamName);
            }
        },

        resetDefaultStreamForm: function() {
            this.$('#stream-name').val('');
            this.defaultStreamForm();
        },

        customStreamForm: function(event) {
            if (event !== null && typeof event !== 'undefined') {
                event.preventDefault();
            }
            var streamName = this.$('#stream-name').val();
            this.$el.html(utils.templateHtml.customStreamTemplate);
            // Keep the stream name if it is already typed
            if (typeof streamName !== 'undefined') {
                this.$('#create-msgstream-form #stream-name').attr('value', streamName);
            }
            this.$('#stream-source').html(utils.templateHtml.sourceSelectTemplate);
            this.$('#stream-processor').html(utils.templateHtml.processorSelectTemplate);
            this.$('#stream-sink').html(utils.templateHtml.sinkSelectTemplate);
        },

        resetCustomStreamForm: function() {
            this.$('#stream-name').val('');
            this.customStreamForm();
        },

        createDefaultStream: function(event) {
            event.preventDefault();
            var streamName = this.$('#stream-name').val().trim();
            var streamDefinition = this.$('#stream-definition').val().trim();
            var createStream = router.createStream(streamName, streamDefinition, function() {
                router.refresh('streams');
                utils.showSuccessMsg(strings.scheduleJobSuccess);
            });
            when(createStream).then(this.resetDefaultStreamForm());
        },

        // Util to get module properties for the selected module type
        getModuleProps: function(moduleDomId, parameters) {
            var module_props = " ";
            // Get module properties based on the form inputs
            if (typeof parameters !== 'undefined' && parameters.length !== 0) {
                // Iterate over all the available parameters
                $.each(parameters, function(index, property) {
                    var property_name = property.name.replace(/\./g, "\\.");
                    var typed_value = $(moduleDomId + ' > #' + property_name + ' input').val();
                    module_props = module_props + "--" + property.name + "=" + typed_value + " ";
                });
            }
            return module_props;
        },

        selectSourceParams: function(event) {
            event.preventDefault();
            this.$('#stream-source > .input-prepend').remove();
            var selectedSource = this.$(event.target).val();
            var parameters = utils.getModuleParameters("source", selectedSource);
            utils.appendParameters(parameters, $('#stream-source'));
        },

        selectProcessorParams: function(event) {
            event.preventDefault();
            this.$('#stream-processor > .input-prepend').remove();
            var selectedProcessor = this.$(event.target).val();
            var parameters = utils.getModuleParameters("processor", selectedProcessor);
            utils.appendParameters(parameters, $('#stream-processor'));
        },

        selectSinkParams: function(event) {
            event.preventDefault();
            this.$('#stream-sink > .input-prepend').remove();
            var selectedSink = this.$(event.target).val();
            var parameters = utils.getModuleParameters("sink", selectedSink);
            utils.appendParameters(parameters, $('#stream-sink'));
        }
    });
    return CreateStream;
});
