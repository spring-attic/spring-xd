Spring XD User Interface Module
===============================

This is the *Spring XD User Interface (UI) Module*. This module uses [AngularJS][]. In order to follow some common conventions, this module has been using [Yeoman][] to kickstart the project. Specifically, the [AngularJS generator][] has been used. However, instead of [ngRoute][], [AngularUI Router][] is used to provide nested view support.

Proposing a few deviations from the [Yeoman][] [AngularJS generator][] conventions:

* Add E2E Testing (may consider Protractor later)
* Name Controllers with the **Controller** suffix rather than **Ctrl**
* Use [AngularUI Router][]

# Building the Module

2 Build Tool Chains are supported. Primarily, the *Spring XD UI Module* uses [Grunt][] ([Node.js][]-based) and [Bower][] for managing dependencies and the execution if the build. In order to integrated with the larger *Spring XD* build process, [Gradle][] can also be used to execute the build (executing [Grunt][] underneath)

## Building the Project using Grunt

	$ grunt

This will invoke the default task. The default task is equivalent of executing:

	$ grunt build

This will trigger the following [Grunt][] tasks to be executed:

* clean:dist
* newer:jshint
* bower:install
* bower-install
* less
* useminPrepare
* concurrent:dist
* autoprefixer
* concat
* ngmin
* copy:dist
* cdnify
* cssmin
* uglify
* rev
* usemin
* htmlmin
* test:unit'

In order to also execute the End-to-End (E2E) tests, execute the build using:

	$ grunt builde2e

### Running Tests

	$ grunt test

### E2E Testing

	$ grunt test:e2e

### Running the Project for Development

	$ grunt serve

The local browser window should open automatically.

## Building the Project using Gradle

When using [Gradle][] execute:

	$ gradle build

This will execute the following tasks:

* npmInstall (Install [Node.js][] dependencies defined in `package.json`)
* installGrunt (Install [Grunt[]])

This will implicitly also install a local [Node.js][] instance.

# Dependency Management using Bower

## Search for dependencies:

The following command will search for a dependency called `angular-ui-route`.

	$ bower search angular-ui-route

## Install Bower dependency

Install that dependency and save it to `bower.json`:

	$ bower install angular-ui-router --save

Inject your dependencies into your `index.html` file:

	$ grunt bower-install

# Dependency Management using Node (used by Grunt)

## Install Build Dependency

	$ npm install --save-dev grunt-contrib-less

## How to Update Node.js dependencies in package.json

Use [https://github.com/tjunnone/npm-check-updates](https://github.com/tjunnone/npm-check-updates)

# Additional Testing Information

When executing E2E tests using e.g. Chrome, make sure the relevant paths to your test browser are setup, e.g.:

	$ export CHROME_BIN="/MyApps/Google Chrome.app/Contents/MacOS/Google Chrome"

[AngularJS]: http://angularjs.org/
[AngularJS generator]: https://github.com/yeoman/generator-angular
[Yeoman]: http://yeoman.io/
[ngRoute]: http://docs.angularjs.org/api/ngRoute
[AngularUI Router]: https://github.com/angular-ui/ui-router
[Grunt]: http://gruntjs.com/
[Bower]: http://bower.io/
[Node.js]: http://nodejs.org/
