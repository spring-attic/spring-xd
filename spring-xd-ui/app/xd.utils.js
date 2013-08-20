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
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 */

/*
 * xd util functions
 */

define(['underscore', 'when', 'jquery', 'xd.conf'], function(_, when, $, conf) {

    var utils = {};
    /*
     *    Get the html template file from the given url
     */
    utils.getTemplate = function(url) {
        // Check if the template is already loaded
        if (!conf.templates[url]) {
            var response = $.ajax(url, {
                dataTypeString: 'html',
                async: false  // TODO synchronous xhr request BAD!
            });
            conf.templates[url] = response.responseText;
        }
        return conf.templates[url];
    };

    /*
     *  Generic error message
     */
    utils.showError = function(message) {
        console.error(message);
        $(window.document).trigger('error', message);
    };

    /*
     * Pre loaded HTML templates
     */
    utils.templateHtml = {
        defaultStreamTemplate: _.template(utils.getTemplate(conf.templates.createDefaultStream))(),
        customStreamTemplate: _.template(utils.getTemplate(conf.templates.createCustomStream))(),
        tapStreamTemplate: _.template(utils.getTemplate(conf.templates.tapStream))(),
        scheduleJobTemplate: _.template(utils.getTemplate(conf.templates.scheduleJob))()
    };

    /*
     *    Set parameter for the selected module option
     */
    utils.appendParameters = function(parameters, el) {
        if (typeof parameters !== 'undefined' && parameters.length !== 0) {
            parameters.forEach(function(property) {
                el.append(_.template(utils.getTemplate(conf.templates.streamParameter), {
                    parameter: property.name,
                    defaultValue: property.defaultValue
                }));
            });
        }
    };

    /*
     * Util function for handling alert msg
     */
    utils.updateStatus = function(status, message) {
        return _.template(utils.getTemplate(conf.templates.statusMsg), {
            status: status,
            message: message
        });
    };

    /*
     * Stream creation success
     */
    utils.showSuccessMsg = function(message) {
        $(window.document).trigger('success', message);
    };

    /*
     * Stream creation error
     */
    utils.showErrorMsg = function(message) {
        console.error(message);
        message = message[0] ? message[0] : message;
        message = message.message ? message.message : message;
        $(window.document).trigger('error', message);
    };

    return utils;
});