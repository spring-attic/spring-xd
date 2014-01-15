'use strict';

angular.module('App.Controllers', [])
	.controller('ListDefinitionController',
		function ($scope, $http, JobDefinitions, $log, promiseTracker, $q, $timeout, growl, JobDefinitionService) {

		var testPromise = $q.defer();

		promiseTracker('trackerName').addPromise(testPromise.promise);
		$timeout(function(){
			testPromise.resolve();
		}, 1000);

		JobDefinitions.get(function(data) {
			$log.info(data);
			$scope.jobDefinitions = data.content;
		}, function(error) {
			$log.error('Error fetching data. Is the XD server running?');
			$log.error(error);
			growl.addErrorMessage('Error fetching data. Is the XD server running?');
		});

		$scope.deployJob = function (jobDefinition) {
			$log.info('Deploying Job ' + jobDefinition.name);
			$log.info(JobDefinitionService);
			JobDefinitionService.deploy(jobDefinition);
		};
		$scope.undeployJob = function (jobDefinition) {
			$log.info('Undeploying Job ' + jobDefinition.name);
			$log.info(JobDefinitionService);
			JobDefinitionService.undeploy(jobDefinition);
		};
	})
	.controller('ListJobDeploymentsController', function($scope, $http, JobDeployments, $log, $state, growl) {
			JobDeployments.getArray(function(data) {
				$log.info(data);
				$scope.jobDeployments = data;

				$scope.launchJob = function (item) {
					$log.info('Launching Job: ' + item.name);
					$state.go('home.jobs.deployments.launch', {jobName: item.name});
				};

			}, function(error) {
				$log.error('Error fetching data. Is the XD server running?');
				$log.error(error);
				growl.addErrorMessage('Error fetching data. Is the XD server running?');
			});
		})
	.controller('ListJobExecutionsController', [ '$scope', '$http',
		'JobExecutions', '$log', function($scope, $http, JobExecutions, $log) {
			JobExecutions.getArray(function(data) {
				$log.info(data);
				$scope.jobExecutions = data;
			});
		} ])
	.controller('LaunchJobController', [ '$scope', '$http', '$log', '$state', '$stateParams', 'growl', '$location',
			'JobLaunchService', function($scope, $http, $log, $state, $stateParams, growl, $location, JobLaunchService) {

			var jobLaunchRequest = $scope.jobLaunchRequest = {
				jobName: $stateParams.jobName,
				jobParameters:[]
			};

			$log.info($stateParams);

			$scope.addParameter = function() {
				jobLaunchRequest.jobParameters.push({key:'new', value:''});
			};

			$scope.removeParameter = function(jobParameter) {
				    for (var i = 0, ii = jobLaunchRequest.jobParameters.length; i < ii; i++) {
				      if (jobParameter === jobLaunchRequest.jobParameters[i]) {
				        $scope.jobLaunchRequest.jobParameters.splice(i, 1);
				      }
				    }
				  };

			$scope.dataTypes = [
				{id:1, key:'string', name: 'String', selected: true},
				{id:2, key:'date',   name: 'Date'},
				{id:3, key:'long',   name: 'Long'},
				{id:4, key:'double', name: 'Double'}
			];

			$scope.cancelJobLaunch = function () {
				$log.info('Cancelling Job Launch');
				$state.go('home.jobs.deployments');
			};

			$scope.launchJob = function (jobLaunchRequest) {
				$log.info('Launching Job ' + jobLaunchRequest.jobName);
				JobLaunchService.convertToJsonAndSend(jobLaunchRequest);
				$location.path('/jobs/deployments');
			};
		}]);


