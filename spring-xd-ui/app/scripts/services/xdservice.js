'use strict';

var xdService = angular.module('xdService', [
	'ngResource',
	'angular-growl'
]);

// angular.module('xdApp')
// 	.controller('ListDefinitionController', function ($scope, $http) { //JobDefinitions, 

xdService.factory('JobDefinitions', function($resource, $rootScope) {
	return $resource($rootScope.xdAdminServerUrl + '/jobs.json?deployments=true', {}, {
		query : {
			method : 'GET',
			isArray : true
		}
	});
});

xdService.factory('JobDefinitionService', function($resource, $log, $rootScope) {
	return {
		deploy: function(jobDefinition){
			$log.info('Deploy Job ' + jobDefinition.name);
			$resource($rootScope.xdAdminServerUrl + '/jobs/' + jobDefinition.name, { 'deploy' : true }, {
				deploy: { method: 'PUT' }
			}).deploy();
		},
		undeploy: function(jobDefinition){
			$log.info('Undeploy Job ' + jobDefinition.name);
			$resource($rootScope.xdAdminServerUrl + '/jobs/' + jobDefinition.name, { 'deploy' : false }, {
				undeploy: { method: 'PUT' }
			}).undeploy();
		}
	};
});

xdService.factory('JobDeployments', function($resource, $rootScope) {
	return $resource($rootScope.xdAdminServerUrl + '/batch/jobs.json', {}, {
		getArray: {method: 'GET', isArray: true}
	});
});

xdService.factory('JobExecutions', function($resource, $rootScope) {
	return $resource($rootScope.xdAdminServerUrl + '/batch/executions', {}, {
		getArray: {method: 'GET', isArray: true}
	});
});

xdService.factory('JobLaunchService', function($resource, growl, $rootScope) {
	return {
		convertToJsonAndSend: function(jobLaunchRequest) {

			console.log('Converting to Json: ' + jobLaunchRequest.jobName);
			console.log(jobLaunchRequest);
			//console.log('Model Change: ' + JSON.stringify(jobLaunchRequest.toJSON()));

			var jsonData = {};
			jobLaunchRequest.jobParameters.forEach(function(jobParameter) {

				var key = jobParameter.key;
				var value = jobParameter.value;
				var isIdentifying = jobParameter.isIdentifying;
				var dataType = jobParameter.type;
				var dataTypeToUse = '';

				if(typeof dataType !== 'undefined') {
					dataTypeToUse = '(' + dataType + ')';
				}

				if (isIdentifying) {
					jsonData['+' + key + dataTypeToUse] = value;
				}
				else {
					jsonData['-' + key + dataTypeToUse] = value;
				}
			});
			console.log(jsonData);
			var jsonDataAsString = JSON.stringify(jsonData);

			console.log(jsonDataAsString);

			this.launch(jobLaunchRequest.jobName, jsonDataAsString);
		},
		launch: function(jobName, jsonDataAsString){
			console.log('Do actual Launch...');
			$resource($rootScope.xdAdminServerUrl + '/jobs/' + jobName + '/launch', { 'jobParameters' : jsonDataAsString }, {
				launch: { method: 'PUT' }
			}).launch().$promise.then(
				function(){
					growl.addSuccessMessage('Job ' + jobName + ' launched.');
				},
				function( data ){
					console.error(data);
					growl.addErrorMessage('Yikes, something bad happened while launching job ' + jobName);
				}
			);
			
		}
	};
});

