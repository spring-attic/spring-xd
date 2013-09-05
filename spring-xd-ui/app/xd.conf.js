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
* Common xd configuration
*/

define(function() {
    'use strict';

    var URL_ROOT = 'http://localhost:8080/';
    var conf = {
        urlRoot: URL_ROOT,
        moduleRoot: URL_ROOT + 'modules/',
        templateRoot: './templates/',
        // Set the streams refresh interval
        refreshInterval: 4000,
        // Set the streams status refresh interval
        streamStatusRefreshInterval: 8000
    };
    conf.url = {
        sources: conf.moduleRoot + 'source',
        processors: conf.moduleRoot + 'processor',
        sinks: conf.moduleRoot + 'sink'
    };

    /*
     * xd app html template file urls
     */
    conf.templates  = {
        statusMsg : conf.templateRoot + 'container/status-msg.tpl',
        navbar : conf.templateRoot + 'container/navbar.tpl',
        dashboard : conf.templateRoot + 'dashboard/dashboard.tpl',
        pagination: conf.templateRoot + 'dashboard/pagination.tpl',
        artifactsList: conf.templateRoot + 'dashboard/artifacts-list.tpl',
        artifactsListItem: conf.templateRoot + 'dashboard/artifacts-list-item.tpl',
        streamDetail: conf.templateRoot + 'dashboard/stream-detail.tpl',
        createDefaultStream : conf.templateRoot + 'stream/default-stream.tpl',
        streamParameter: conf.templateRoot + 'stream/module/parameter-text.tpl',
        tapStream : conf.templateRoot + 'stream/tap-stream.tpl',
        scheduleJob : conf.templateRoot + 'job/schedule-job.tpl',

        batchList: conf.templateRoot + 'batch/batch-list.tpl',
        batchDetails: conf.templateRoot + 'batch/batch-details.tpl'

    };

    return conf;
});