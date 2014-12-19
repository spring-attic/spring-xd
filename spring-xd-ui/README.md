Spring XD User Interface Module
===============================

This is the *Spring XD User Interface (UI) Module*. This module uses [AngularJS][]. The project started out using [Yeoman][] (with the [AngularJS generator][]) to follow some common conventions. Instead of [ngRoute][], [AngularUI Router][] is used to provide nested view support.

For E2E Testing we use [Protractor][].

# Building the Module

2 Build Tool Chains are supported. Primarily, the *Spring XD UI Module* uses [Grunt][] ([Node.js][]-based) and [Bower][] for managing dependencies and the execution of the build. In order to integrate with the larger *Spring XD* build process, [Gradle][] can also be used to execute the build (executing [Grunt][] underneath)

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

### Running Tests

	$ grunt test

### E2E Testing

In order to also execute the End-to-End (E2E) tests, please execute the UI build using:

	$ grunt serve
	
In one console window and in another window execute:

	$ grunt protractor:run

Please ensure that a Spring XD server instance is running at `http://localhost:9393/`.

### Running the Project for Development

	$ grunt serve

The local browser window should open automatically.

## Building the Project using Gradle

When using [Gradle][] execute:

	$ gradle setupUI

This will execute the following tasks:

* npmInstall (Install [Node.js][] dependencies defined in `package.json`)
* installGrunt (Install [Grunt[]])
* grunt_build

This will implicitly also install a local [Node.js][] instance.

### Other Gradle tasks

* **ui_test** - Runs all tests (incl. E2E) with an XD Single Node instance in the background
* **cleanUI** - clean Node Modules, Bower files, clean dist folder

| **Important** If End-to-End (E2E) testing fails, you may have to execute `./node_modules/protractor/bin/webdriver-manager update`.

# Dependency Management using Bower

[Bower][] is used for managing UI dependencies.

## Search for dependencies:

The following command will search for a dependency called `angular-ui-route`.

	$ bower search angular-ui-route

## Install Bower dependency

Install that dependency and save it to `bower.json`:

	$ bower install angular-ui-router --save

Inject your dependencies into your `index.html` file:

	$ grunt bower-install

## Install Build Dependency

	$ npm install --save-dev grunt-contrib-less

## How to Update Node.js dependencies in package.json

Use [https://github.com/tjunnone/npm-check-updates](https://github.com/tjunnone/npm-check-updates)

# E2E Testing using Protractor

End-to-End tests are executed using [Protractor][]. By default we use [ChromeDriver][].

| **Note**: Chrome Driver "expects you to have Chrome installed in the default location for each system", e.g. for Mac it is: `/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome`. For further details, please see: https://code.google.com/p/selenium/wiki/ChromeDriver

Please be also aware of the following [grunt-protractor-runner](https://github.com/teerapap/grunt-protractor-runner) plugin issue: https://github.com/teerapap/grunt-protractor-runner/issues/45

For example, you may encounter an issue such as the following:

````
Starting Protractor...
.
-------------------------------------------------------------------

/usr/local/share/npm/lib/node_modules/protractor/lib/driverProviders/local.js:42
        throw new Error('Could not find chromedriver at ' +
              ^
Error: Could not find chromedriver at /usr/local/share/npm/lib/node_modules/protractor/selenium/chromedriver.exe
    at LocalDriverProvider.addDefaultBinaryLocs_ (/usr/local/share/npm/lib/node_modules/protractor/lib/driverProviders/local.js:42:15)
...
````

Therefore, when running E2E tests, you may need to execute first: `./node_modules/protractor/bin/webdriver-manager update`.
Please pay also special attention whether you're using a **local** or global **global** protractor instance.

In order to improve the situation we may consider adding a special Grunt task for that as illustrated here: http://gitelephant.cypresslab.net/angular-js/commit/2ed4ad55022f6e5519617a3797649fe1e68f3734

You should now be able to execute the E2E tests using grunt:

	$ grunt test:e2e

You should be able to see console output similar to the following:

```
Started connect web server on http://0.0.0.0:8000

Running "protractor:run" (protractor) task
Starting selenium standalone server...
Selenium standalone server started at http://10.0.1.4:60164/wd/hub
.....................................

Finished in 42.012 seconds
37 tests, 55 assertions, 0 failures

Shutting down selenium standalone server.

Done, without errors.


Execution Time (2014-06-10 13:59:50 UTC)
concurrent:server     3s  ▇▇▇▇▇▇▇ 6%
protractor:run     49.2s  ▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇ 94%
Total 52.2s
```

## Protractor in Debug Mode

During development you may want to run E2E tests while working on the UI portions of the application. For this purpose you can use the provided script `e2e-test.sh` to run the E2E tests
against a running Admin UI instance. As a requirement you need to have running:

* Admin UI Server
* Admin UI started from the console using `grunt serve`

You may also need to install `protractor` using

	$ npm install -g protractor

To execute the E2E tests, execute `./e2e-test.sh` via the console and all E2E tests should run.
`
This option also allows you to run Protractor in `debug` mode allowing you to pause the execution of individual tests. For this to happen, execute:

	$ ./e2e-test.sh debug

You should see the following console output:

```
Starting Protractor...
.
-------------------------------------------------------------------
Starting selenium standalone server...
Hit SIGUSR1 - starting debugger agent.
debugger listening on port 5858
connecting... ok
break in timers.js:77
  75 }
  76
  77 function listOnTimeout() {
  78   var msecs = this.msecs;
  79   var list = this;
debug>
```

Please press `c` to continue with the execution. All E2E tests should now execute. In order to set 'breakpoints', add `browser.debugger();` at the appropriate places within your E2E tests. Fur further information see: https://github.com/angular/protractor/blob/master/docs/debugging.md

[AngularJS]: http://angularjs.org/
[AngularJS generator]: https://github.com/yeoman/generator-angular
[Yeoman]: http://yeoman.io/
[ngRoute]: http://docs.angularjs.org/api/ngRoute
[AngularUI Router]: https://github.com/angular-ui/ui-router
[Gradle]: http://www.gradle.org/
[Grunt]: http://gruntjs.com/
[Bower]: http://bower.io/
[Node.js]: http://nodejs.org/
[Protractor]: https://github.com/angular/protractor
[ChromeDriver]: https://code.google.com/p/selenium/wiki/ChromeDriver

