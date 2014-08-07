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

'use strict';

/**
 * @author Gunnar Hillert
 */
describe('Stream Tests', function() {

  beforeEach(function() {
    browser.ignoreSynchronization = true;
  });
  afterEach(function() {
    browser.ignoreSynchronization = false;
  });

  describe('When I navigate to "/#/streams"', function() {
    it('the main header should be labelled "Streams"', function() {
      browser.get('#/streams/definitions');
      expect(element(by.css('#xd-content h1')).getText()).toEqual('Streams');
    });
    it('there should be 1 tab which is active', function() {
      expect(element.all(by.css('#xd-content ul li')).count()).toEqual(1);
      expect(element.all(by.css('#xd-content ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Definitions"', function() {
      expect(element(by.css('#xd-content ul li.active a')).getText()).toEqual('Definitions');
    });
    it('there should be a table with 4 columns', function() {
      expect(element.all(by.css('#xd-content table thead th')).count()).toEqual(4);
    });
    it('the first column is labelled "Name"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(1)')).getText()).toEqual('Name');
    });
    it('the second column is labelled "Definition"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(2)')).getText()).toEqual('Definition');
    });
    it('the third column is labelled "Status"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(3)')).getText()).toEqual('Status');
    });
    it('the forth column is labelled "Actions"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(4)')).getText()).toEqual('Actions');
    });
    it('the "Definitions" Tab should have a "Quick Filter" search input field', function() {
      expect(element(by.css('#filterTable')).isPresent()).toBe(true);
    });
    it('the "Definitions Tab Quick Filter" should have a placeholder text of "Quick filter"', function() {
      expect(element(by.css('#filterTable')).getAttribute('placeholder')).toMatch('Quick filter');
    });
  });

});
