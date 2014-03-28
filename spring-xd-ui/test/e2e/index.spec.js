'use strict';

describe('Tests', function() {

  var ptor = protractor.getInstance();

  beforeEach(function() {
    ptor.get('/');
    ptor.waitForAngular();
  });

  describe('When I navigate to the root URL "/"', function() {
    it('the app should redirect to "#/jobs/definitions"', function() {
      ptor.get('/');
      ptor.waitForAngular();
      expect(ptor.getCurrentUrl()).toContain('/#/jobs/definitions')
      expect(ptor.getTitle()).toBe('Spring XD');
    });
  });

  describe('When I navigate to some non-existing URL, e.g. "/#/foobar"', function() {
    it('the app should redirect to "#/jobs/definitions"', function() {
      ptor.get('/#/foobar');
      expect(ptor.getCurrentUrl()).toContain('/jobs/definitions');
    });
  });

  describe('When I navigate to "/jobs/definitions"', function() {
    it('there should be 4 tabs of which one is active', function() {
      ptor.get('#/jobs/definitions');
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Definitions"', function() {
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Definitions');
    });
  });

  describe('When I navigate to "/jobs/deployments"', function() {
    it('there should be 3 tabs of which one is active', function() {
      ptor.get('#/jobs/deployments');
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Deployments"', function() {
      ptor.get('#/jobs/deployments');
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Deployments');
    });
  });

  describe('When I navigate to "/jobs/executions"', function() {
    it('there should be 3 tabs of which one is active', function() {
      ptor.get('#/jobs/executions');
      expect(element.all(by.css('#xd-jobs ul li')).count()).toEqual(4);
      expect(element.all(by.css('#xd-jobs ul li.active')).count()).toEqual(1);
    });
    it('the active tab should be labelled "Executions"', function() {
      ptor.get('#/jobs/executions');
      expect(element(by.css('#xd-jobs ul li.active a')).getText()).toEqual('Executions');
    });
  });

  describe('When I navigate to "/#/about"', function() {
    it('the main header should be labelled "About"', function() {
      ptor.get('#/about');
      expect(element(by.css('#xd-content h1')).getText()).toEqual('About');
    });
  });
});
