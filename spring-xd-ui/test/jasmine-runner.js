/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @author Ken Mayer
 * @author Ilayaperumal Gopinathan
 */
var page = require("webpage").create();
var system = require('system');
var url;

if (system.args.length != 2) {
	console.log('Usage: jasmine-runner.js <URL>');
	phantom.exit(127);
}

// Handle window.console.log(msg);
page.onConsoleMessage = function() {
    console.log(arguments[0]);
    if (arguments[0] == "ConsoleReporter finished: fail") {
      setTimeout(function() { phantom.exit(1); }, 100);
    }
    if (arguments[0] == "ConsoleReporter finished: success") {
      setTimeout(function() { phantom.exit(0); }, 100);
    }
};

console.log("Opening: " + system.args[1]);
page.open(system.args[1]);

