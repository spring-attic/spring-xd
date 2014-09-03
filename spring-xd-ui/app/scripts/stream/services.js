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

/**
 * XD Stream services.
 *
 * @author Ilayaperumal Gopinathan
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdStreamsAdmin.services', [])
      .factory('StreamService', function ($resource, $rootScope, $log) {
        return {
          getDefinitions: function () {
            return $resource($rootScope.xdAdminServerUrl + '/streams/definitions.json', {}, {
              query: {
                method: 'GET'
              }
            });
          },
          deploy: function (name, deploymentProperties) {
            $log.info('Deploy Stream ' + name);
            return $resource($rootScope.xdAdminServerUrl + '/streams/deployments/' + name, null, {
              deploy: {
                method: 'POST',
                params: {
                  properties: deploymentProperties
                }
              }
            }).deploy();
          },
          undeploy: function (streamDefinition) {
            $log.info('Undeploy Stream ' + streamDefinition.name);
            return $resource($rootScope.xdAdminServerUrl + '/streams/deployments/' + streamDefinition.name, null, {
              undeploy: { method: 'DELETE' }
            }).undeploy();
          },
          destroy: function (streamDefinition) {
            $log.info('Undeploy Stream ' + streamDefinition.name);
            return $resource($rootScope.xdAdminServerUrl + '/streams/definitions/' + streamDefinition.name, null, {
              destroy: { method: 'DELETE' }
            }).destroy();
          },
          getModulesFromDSL: function (streamName, dsl) {
            $log.info('Get modules from DSL for stream: ' + streamName);
            return $resource($rootScope.xdAdminServerUrl + '/dslparser', {dsl: dsl}, {
              getModules: { method: 'GET', isArray: true }
            }).getModules();
          }
        };
      });
});
