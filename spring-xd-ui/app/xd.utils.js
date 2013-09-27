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

/*global define */
define(['underscore', 'jquery'], function(_, $) {
    'use strict';

    return function (statusMsgTemplate) {
        var utils = {};

        /*
         *  Generic error message
         */
        utils.showError = function(message) {
            console.error(message);
            $(window.document).trigger('error', message);
        };

        /*
         *    Set parameter for the selected module option
         */
        utils.appendParameters = function(parameters, el) {
            if (typeof parameters !== 'undefined' && parameters.length !== 0) {
                parameters.forEach(function(property) {
                    el.append(_.template(statusMsgTemplate, {
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
            return _.template(statusMsgTemplate, {
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
    };
});