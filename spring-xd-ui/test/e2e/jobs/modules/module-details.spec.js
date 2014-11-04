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
describe('Tests for the Module Details Page', function() {

  beforeEach(function() {
    browser.ignoreSynchronization = true;
  });
  afterEach(function() {
    browser.ignoreSynchronization = false;
  });

  describe('When I navigate to the Module Details URL for the "filejdbc" module - "#/jobs/modules/filejdbc"', function() {
    it('The page title should be "Module Details"', function() {
      browser.get('#/jobs/modules/filejdbc');
      expect(browser.getCurrentUrl()).toContain('#/jobs/modules/filejdbc')
      expect(browser.getTitle()).toBe('Module Details');
    });
    it('there should be a table with 4 columns', function() {
      expect(element.all(by.css('#xd-content table thead th')).count()).toEqual(4);
    });
    it('the first column is labelled "Name"', function() {
      browser.driver.sleep(2000);
      expect(element(by.css('#xd-content table thead th:nth-child(1)')).getText()).toEqual('Name');
    });
    it('the second column is labelled "Type"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(2)')).getText()).toEqual('Type');
    });
    it('the third column is labelled "Default Value"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(3)')).getText()).toEqual('Default Value');
    });
    it('the fourth column is labelled "Description"', function() {
      expect(element(by.css('#xd-content table thead th:nth-child(4)')).getText()).toEqual('Description');
    });
    it('if the user clicks the "back" button, the module list page should be loaded', function() {
      var backButton = element(by.css('#back-button'));
      expect(backButton.isPresent()).toBe(true);
      expect(backButton.getText()).toEqual('Back');
      backButton.click();
      expect(browser.getCurrentUrl()).toContain('/jobs/modules');
    });
  });
});
