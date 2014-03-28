
define([
  'angular',
  'angularMocks',
  'app'
], function(angular, mocks, app) {
  'use strict';

  describe('Unit: Testing Job Controllers', function() {

    beforeEach(function() {
      angular.mock.module('xdAdmin');
    });

    it('should have a ListDefinitionController', inject(function($rootScope, $controller) {
      var controller = $controller('ListDefinitionController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));

    it('should have a ListJobDeploymentsController', inject(function($rootScope, $controller) {
      var controller = $controller('ListJobDeploymentsController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));

    it('should have a ListJobExecutionsController', inject(function($rootScope, $controller) {
      var controller = $controller('ListJobExecutionsController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));

    it('should have a ModuleController', inject(function($rootScope, $controller) {
      var controller = $controller('ModuleController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));

    it('should have a JobLaunchController', inject(function($rootScope, $controller) {
      var controller = $controller('JobLaunchController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));

    it('should have a ModuleDetailsController', inject(function($rootScope, $controller) {
      var controller = $controller('ModuleDetailsController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
      expect(controller).toBeDefined();
    }));
  });
});

