/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

