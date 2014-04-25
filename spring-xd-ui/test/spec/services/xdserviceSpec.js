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
], function(angular) {
  'use strict';

  describe('Unit: Testing XD Services', function() {
    beforeEach(function() {
      angular.mock.module('xdAdmin');
    });

    it('should contain a JobDefinitions service', inject(function(JobDefinitions) {
      expect(JobDefinitions).toBeDefined();
    }));

    it('should contain a JobDefinitionService service', inject(function(JobDefinitionService) {
      expect(JobDefinitionService).toBeDefined();
    }));

    it('should contain a JobDeployments service', inject(function(JobDeployments) {
      expect(JobDeployments).toBeDefined();
    }));

    it('should contain a JobExecutions service', inject(function(JobExecutions) {
      expect(JobExecutions).toBeDefined();
    }));

    it('should contain a JobLaunchService service', inject(function(JobLaunchService) {
      expect(JobLaunchService).toBeDefined();
    }));
  });
});

