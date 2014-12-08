/*
 * Copyright 2014 the original author or authors.
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

/**
 * Testing the "xdFormatStream" Directive
 *
 * @author Gunnar Hillert
 */
define([
  'angular',
  'angularMocks',
  'xregexp'
], function(angular) {
  'use strict';

  describe('Unit: Testing xdFormatStream Directive', function() {

    var $scope, element;

    beforeEach(function() {
      angular.mock.module('xdJobsAdmin');
    });

    beforeEach(inject(function($compile, $rootScope) {
      $scope = $rootScope;
      element = angular.element(
       '<div id="myDiv" xd-format-stream="myVal"></div>'
      );
      $scope.myVal = 'hello world';
      $compile(element)($scope);
      $scope.$digest();
    }));

    it('Make sure the basic test fixture setup works', inject(function() {
      var elementValue = element.text();
      expect(elementValue).toEqual('hello world');
    }));
    it('Changing the scope variable "myVal" should result in a changed element contents', inject(function() {
      $scope.myVal = 'new foo bar';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('new foo bar');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password=12345678';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password=********');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value (containing a ".") masked', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password=ab.cd.efghi';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password=***********');
    }));
    it('A basic stream definition parameter (parameter in middle of stream) called "password" should have its value masked', inject(function() {
      $scope.myVal = 'filejdbc --password=12345678 --driverClassName=org.postgresql.Driver';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --password=******** --driverClassName=org.postgresql.Driver');
    }));
    it('The password parameter can be upper-case, lower-case or mixed-case', inject(function() {
      $scope.myVal = 'mystream -- password=12 --PASSword=  1234 --PASSWORD=  1234';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('mystream -- password=** --PASSword=  **** --PASSWORD=  ****');
    }));
    it('The password parameter value in Russian (UTF) should be masked also', inject(function() {
      $scope.myVal = 'mystream --password=Берлин';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('mystream --password=******');
    }));
    it('A basic stream definition parameter (parameter at end) called "passwwd" should have its value masked', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --passwd=12345678';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --passwd=********');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains an underscore.', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password=12345678_abcd';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password=*************');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains a currency symbols.', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password=12345678$a€bc¥d';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password=***************');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value is wrapped in quotation marks.', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password="abcd"';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password="****"');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains spaces.', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password="ab  cd"';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password="******"');
    }));
    it('A basic stream definition parameter (parameter at end) called "password" should have its value masked. The value contains dashes.', inject(function() {
      $scope.myVal = 'filejdbc --driverClassName=org.postgresql.Driver --password="ab---cd"';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --driverClassName=org.postgresql.Driver --password="*******"');
    }));
    it('A basic stream definition parameter (parameter in middle of stream) called "password" with value wrapped in quotes should have its value masked', inject(function() {
      $scope.myVal = 'filejdbc --password="12345678" --driverClassName="org.postgresql.Driver"';
      $scope.$digest();
      var elementValue = element.text();
      expect(elementValue).toEqual('filejdbc --password="********" --driverClassName="org.postgresql.Driver"');
    }));

  });
});

