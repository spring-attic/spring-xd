# Spring-XD UI

This project contains the Spring-XD UI and this document describes how to use it.

## Running the UI

1. Start the XD server on a local host
2. Navigate to http://localhost:8080/admin-ui

## What is implemented now

Currently, you can do the following:

1. Create batch jobs
2. Launch batch jobs
3. Launch batch jobs with custom parameters
4. View a list of batch jobs already launched

## How to run the tests

The test suite is written using [Jasmine](http://pivotal.github.io/jasmine/). 
There are two ways to run the tests:

1. In the browser. Open up the test/SpecRunner.html file.
2. Headleass.  (requires Phantomjs to be installed) run the following command:

    phantomjs test/run-jasmine.js test/SpecRunner.html

To install phantomjs:

    npm install -g phantiomjs

You will need [npm](https://npmjs.org/) in order to run this command.

## Use of bower

The UI wants to use [bower](http://bower.io/) to manage dependencies.  Problem 
is that bower will checkout entire git repositories and if not careful many 
unnecessary files will be added to the XD git repository.  So, what we do is this:

1. keep bower.json up to date with the dependencies used
2. after each dependency change, run `bower install` from the spring-xd-ui dir
3. this will create a `bower_components` directory
4. copy over all components from `bower_components` to `lib`
5. delete all files (eg- test and documentation files) not used in the UI

To install bower:

    npm install -g bower

You will need [npm](https://npmjs.org/) in order to run this command.

## Use of AMD and requirejs

The UI uses [AMD-style](https://github.com/amdjs/amdjs-api/wiki/AMD) modules 
and [requirejs](http://requirejs.org/) as a module loader.  The AMD configuration 
file is `spring-xd/spring-xd-ui/app/xd.main.js`. The AMD configuration for the 
tests are located at `spring-xd/spring-xd-ui/test/xd.main.spec.js`


## Use of wire

The UI uses [wire.js](https://github.com/cujojs/wire) as a dependency injection framework.  
The wirespec is located at `spring-xd/spring-xd-ui/app/xd.wirespec.js`.

The wire spec specifies each module and all of their dependencies.

## Use of jshint

When making changes to javascript files, you should use an editor that has jshint
integration (eg- Scripted or Sublime text).

## Use of LESS

LESS is used e.g. to create the Bootstrap CSS. In order to compile the LESS files, we use **recess** (from twitter). After installing recess, use the following command:

	recess ./bootstrap.less --compile
	
and to save the **bootstrap.css** into the `css` directory, just redirect the output there

	recess ./bootstrap.less --compile > ../../css/bootstrap.css

