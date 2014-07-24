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

  describe('Unit: Testing XD Shared Services', function() {
    beforeEach(function() {
      angular.mock.module('xdAdmin');
    });

    it('The module name should be retrieved from a definition with parameters.', inject(function(XDUtils) {
      expect(XDUtils.getModuleNameFromJobDefinition).toBeDefined();
      expect(XDUtils.getModuleNameFromJobDefinition('myMod --param=1234')).toEqual('myMod');
    }));

    it('The module name should be retrieved from a definition without parameters.', inject(function(XDUtils) {
      expect(XDUtils.getModuleNameFromJobDefinition).toBeDefined();
      expect(XDUtils.getModuleNameFromJobDefinition('myMod2')).toEqual('myMod2');
    }));
    it('Getting a module name from an undefined definition should casuse an error.', inject(function(XDUtils) {
      expect(XDUtils.getModuleNameFromJobDefinition).toBeDefined();
      expect(function() {
        XDUtils.getModuleNameFromJobDefinition();
      }).toThrow(new Error('jobDefinition must be defined.'));
    }));
    it('Getting a module name from an undefined definition should casuse an error 2.', inject(function(XDUtils) {
      expect(XDUtils.getModuleNameFromJobDefinition).toBeDefined();
      expect(function() {
        XDUtils.getModuleNameFromJobDefinition(undefined);
      }).toThrow(new Error('jobDefinition must be defined.'));
    }));
    it('Getting a module name from a null definition should casuse an error.', inject(function(XDUtils) {
      expect(XDUtils.getModuleNameFromJobDefinition).toBeDefined();
      expect(function() {
        XDUtils.getModuleNameFromJobDefinition(null);
      }).toThrow(new Error('jobDefinition must be defined.'));
    }));
  });
});

