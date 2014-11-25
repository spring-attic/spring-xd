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

'use strict';

/**
 * @author Gunnar Hillert
 */
describe('Tests', function() {

  beforeEach(function() {
    browser.get('/');
    browser.ignoreSynchronization = true;
  });
  afterEach(function() {
    browser.ignoreSynchronization = false;
  });

  describe('When I navigate to the root URL "/"', function() {
    it('the app should redirect to "#/jobs/definitions"', function() {
      browser.get('/');
      browser.driver.sleep(2000);
      expect(browser.getCurrentUrl()).toContain('/#/jobs/definitions')
      expect(browser.getTitle()).toBe('Spring XD');
    });
  });

  describe('When I navigate to some non-existing URL, e.g. "/#/foobar"', function() {
    it('the app should redirect to "#/jobs/definitions"', function() {
      browser.get('/#/foobar');
      expect(browser.getCurrentUrl()).toContain('/jobs/definitions');
    });
  });

  //Modules tab

  describe('When I navigate to "/jobs/modules"', function() {
    it('there should be 4 tabs of which one is active', function() {
      browser.get('#/jobs/modules').then(function() {
        expect(element.all(by.css('#xd-jobs ul.nav-tabs li')).count()).toEqual(4);
        expect(element.all(by.css('#xd-jobs ul.nav-tabs li.active')).count()).toEqual(1);
      });
    });
    it('the active tab should be labelled "Modules"', function() {
      browser.get('#/jobs/modules').then(function() {
        expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Modules');
      });
    });
    it('there should be more than 1 job module being listed', function() {
      browser.get('#/jobs/modules').then(function() {
        browser.driver.sleep(2000);
        expect(element.all(by.css('#xd-jobs table tbody tr')).count()).toBeGreaterThan(4);
      });
    });
    it('there should a job module named filejdbc', function() {
      browser.get('#/jobs/modules');
      browser.driver.sleep(2000);
      expect(element(by.css('#xd-jobs table tbody tr:nth-child(1) td:nth-child(1)')).getText()).toEqual('filejdbc');
    });
    it('When I click on the Create Definition button for module filejdbc, ' +
       'the page should redirect to /jobs/modules/filejdbc/create-definition', function() {
      browser.get('#/jobs/modules').then(function() {
        browser.sleep(2000);
        expect(element(by.css('#xd-jobs table tbody tr:nth-child(1) td:nth-child(2) button')).getAttribute('title')).toMatch('Create Definition');
        element(by.css('#xd-jobs table tbody tr:nth-child(1) td:nth-child(2) button')).click();
        expect(browser.getCurrentUrl()).toContain('/jobs/modules/filejdbc/create-definition');
      });
    });
    it('When I click on the Details button for module filejdbc, ' +
       'the page should redirect to /jobs/modules/filejdbc', function() {
       browser.get('#/jobs/modules').then(function() {
         browser.sleep(2000);
         expect(element(by.css('#xd-jobs table tbody tr:nth-child(1) td:nth-child(3) button')).getAttribute('title')).toMatch('Details');
         element(by.css('#xd-jobs table tbody tr:nth-child(1) td:nth-child(3) button')).click();
         expect(browser.getCurrentUrl()).toContain('/jobs/modules/filejdbc');
       });
     });
  });

  //Definitions tab

  describe('When I navigate to "/jobs/definitions"', function() {
    it('there should be 4 tabs of which one is active', function() {
      browser.get('#/jobs/definitions');
      browser.sleep(1000);
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Definitions"', function() {
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Definitions');
    });
  });

  // Deployments tab

  describe('When I navigate to "/jobs/deployments"', function() {
    it('there should be 4 tabs of which one is active', function() {
      browser.get('#/jobs/deployments');
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Deployments"', function() {
      browser.get('#/jobs/deployments');
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Deployments');
    });
  });

  //Executions tab

  describe('When I navigate to "/jobs/executions"', function() {
    it('there should be 4 tabs of which one is active', function() {
      browser.get('#/jobs/executions');
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Executions"', function() {
      browser.get('#/jobs/executions');
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Executions');
    });
  });

  //About page

  describe('When I navigate to "/#/about"', function() {
    it('the main header should be labelled "About"', function() {
      browser.get('#/about');
      expect(element(by.css('#xd-content h1')).getText()).toEqual('About');
    });
  });
});
