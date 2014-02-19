'use strict';

describe('When I navigate to the root URL "/"', function() {

  it('the app should redirect to "#/jobs/definitions"', function() {
    browser().navigateTo('/');
    expect(browser().location().url()).toBe('/jobs/definitions');
  });

});

describe('When I navigate to some non-existing URL, e.g. "/#/foobar"', function() {

  it('the app should redirect to "#/jobs/definitions"', function() {
    browser().navigateTo('/#/foobar');
    expect(browser().location().url()).toBe('/jobs/definitions');
  });

});

describe('When I navigate to "/jobs/definitions"', function() {

  it('there should be 3 tabs of which one is active', function() {
    browser().navigateTo('#/jobs/definitions');
    expect(element('#xd-jobs ul li').count()).toEqual(3);
    expect(element('#xd-jobs ul li.active').count()).toEqual(1);
  });

  it('the active tab should be labelled "Definitions"', function() {
    expect(element('#xd-jobs ul li.active a').html()).toEqual('Definitions');
  });

});

describe('When I navigate to "/jobs/deployments"', function() {

  it('there should be 3 tabs of which one is active', function() {
    browser().navigateTo('#/jobs/deployments');
    expect(element('#xd-jobs ul li').count()).toEqual(3);
    expect(element('#xd-jobs ul li.active').count()).toEqual(1);
  });

  it('the active tab should be labelled "Deployments"', function() {
    expect(element('#xd-jobs ul li.active a').html()).toEqual('Deployments');
  });

});

describe('When I navigate to "/jobs/executions"', function() {

  it('there should be 3 tabs of which one is active', function() {
    browser().navigateTo('#/jobs/executions');
    expect(element('#xd-jobs ul li').count()).toEqual(3);
    expect(element('#xd-jobs ul li.active').count()).toEqual(1);
  });

  it('the active tab should be labelled "Executions"', function() {
    expect(element('#xd-jobs ul li.active a').html()).toEqual('Executions');
  });

});

describe('When I navigate to "/#/about"', function() {

  it('the main header should be labelled "About"', function() {
    browser().navigateTo('#/about');
    expect(element('#xd-content h1').html()).toEqual('About');
  });

});