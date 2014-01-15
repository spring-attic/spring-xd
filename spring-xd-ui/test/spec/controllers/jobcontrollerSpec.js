'use strict';

describe('Unit: Testing Job Controllers', function() {

  beforeEach(function() {
    angular.mock.module('App');
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

  it('should have a LaunchJobController', inject(function($rootScope, $controller) {
    var controller = $controller('LaunchJobController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
    expect(controller).toBeDefined();
  }));

  it('should have a LaunchJobController', inject(function($rootScope, $controller) {
    var controller = $controller('LaunchJobController', { $scope: $rootScope.$new(), $rootScope: $rootScope });
    expect(controller).toBeDefined();
  }));

});
