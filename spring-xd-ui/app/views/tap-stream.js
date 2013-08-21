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

define(['backbone', 'when', 'xd.utils', 'xd.router', 'xd.strings', 'xd.model'],
function(Backbone, when, utils, router, strings, model) {

    var TapStream = Backbone.View.extend({

         events: {
             'click #tap-stream': 'tapStream'
         },

         tapStreamForm: function() {
             this.$el.html(utils.templateHtml.tapStreamTemplate);
             this.$('#tap-stream-processor').html(utils.templateHtml.processorSelectTemplate);
             this.$('#tap-stream-sink').html(utils.templateHtml.sinkSelectTemplate);
             // Setup source function for typeahead to show streams
             this.$('#stream-to-tap').typeahead({
                 source: function(query, process) {
                     var tapSource = [];
                     model.get('streams').get('artifacts').forEach(function(stream) {
                         tapSource.push(stream.get('name'));
                     });
                     process(tapSource);
                     return tapSource;
                 }
             });
         },

         resetTapStreamForm: function() {
             this.$('#tap-stream-name').val('');
             this.tapStreamForm();
         },

         tapStream: function(event) {
             event.preventDefault();
             // Get stream name
             var tapName = this.$('#tap-stream-name').val().trim();
             var targetStream = this.$('#stream-to-tap').val();
             var definition = this.$('#tap-stream-definition').val().trim();
             var tapStream = router.createTap(tapName, targetStream, definition, function() {
                router.refresh('taps');
                utils.showSuccessMsg(strings.scheduleJobSuccess);
            });
            when(tapStream).then(this.resetTapStreamForm());
         }
     });
     return TapStream;
});