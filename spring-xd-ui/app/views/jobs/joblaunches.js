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
 * View for launching Batch Jobs with Parameters
 *
 * @author Gunnar Hillert
 */
/*global define, _, $ */
define([],
function() {
	'use strict';
	return function(Backbone, Marionette, ModelBinder , model, xdJobLaunchFormTemplate, xdJobLaunchParameterOptionsTemplate, xdJobLaunchParameterTemplate, utils, strings) {

		var DataTypeView = Backbone.View.extend({
			el: $('#rate-editor-container'),
			collection: model.dataTypes,
			initialize: function() {
				this.render();
			},
			render: function() {
				var datatypeTemplate = _.template(xdJobLaunchParameterOptionsTemplate, {
					datatypes: this.collection
				});
				console.log(this.el);
				this.$el.html(datatypeTemplate);
			}
		});

		var JobParameterItemView = Marionette.ItemView.extend({

			_modelBinder:undefined,
			initialize: function(){
				console.log("Initializing the Itemview for the Job Parameters");
				this._modelBinder = new ModelBinder();
			},
			template : _.template(xdJobLaunchParameterTemplate),
			triggers :{
				"click button" : "property:remove",
				"click .add-job-param" : "property:add"
			},
			onRender: function(){

				var region = this.$el.find(".job-params").first();
				console.log(region);

				var dataTypeView = new DataTypeView({
					el: region
				});

				dataTypeView.render();

				this.model.bind('change', function (model) {
					console.log('Model Change: ' + JSON.stringify(model.toJSON()))
				});

				var bindings = {
					key: '[name=key]',
					value: '[name=value]',
					isIdentifying: '[name=isIdentifying]',
					type:{selector:'[name=type]', converter:(new ModelBinder.CollectionConverter(model.dataTypes)).convert},
				};

				this._modelBinder.bind(this.model, this.el, bindings);

			}
		});

		var jobParameterItemView = new JobParameterItemView();

		var CompositeView = Marionette.CompositeView.extend({
			itemView: JobParameterItemView,
			collection: model.jobLaunchRequest.get('jobParameters'),
			el : $( "#job-launch-modal" ),
			initialize: function(){
				console.log("Initializing CompositeView for the JobLaunchRequest");

				this.on("itemview:property:remove", function(view, model){
					console.log("The selected item will be removed." );
					this.collection.remove(view.model);
				});
				this.model.bind('change', function (model) {
					console.log('Collection Model Change: ' + JSON.stringify(model.toJSON()))
				});
			},

//			itemViewOptions: function(model, index) {
//				model.set('itemIndex', index)
//			},

			template : _.template(xdJobLaunchFormTemplate),
			model: model.jobLaunchRequest,
			appendHtml: function(collectionView, itemView){
				collectionView.$("#jobParameters").append(itemView.el);
			},
			events: {
				"click .add-job-param" : "addParameter",
				"click .launch-job" : "launchJob"
			},
			addParameter: function() {
				console.log( "click add!" );
				var jp = model.createJobParameter();
				model.jobLaunchRequest.get('jobParameters').add(jp);
				this.collection.add(jp);
			},
			launchJob: function() {
				console.log( "click launch it!" );
				console.log(this.model);
				this.model.convertToJson();
				$('#job-params-modal').modal('hide');
				model.batchJobs.startFetching();
				utils.showSuccessMsg("The launch request for job '" + this.model.get('jobname') + "' was sent.");
			}
		});

		return CompositeView;

	};
});
