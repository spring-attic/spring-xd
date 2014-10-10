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
      .factory('StreamService', function ($resource, $rootScope, $log, $http) {
        return {
          getDefinitions: function (pageable) {
            var params = {};
            if (pageable === undefined) {
              $log.info('Getting all stream definitions.');
            }
            else {
              $log.info('Getting paged stream definitions', pageable);
              params = {
                'page': pageable.pageNumber,
                'size': pageable.pageSize
              };
            }
            return $resource($rootScope.xdAdminServerUrl + '/streams/definitions.json', params, {
              query: {
                method: 'GET'
              }
            }).get();
          },
          getSingleStreamDefinition: function (streamName) {
            $log.info('Getting single stream definition for job named ' + streamName);
            return $http({
              method: 'GET',
              url: $rootScope.xdAdminServerUrl + '/streams/definitions/' + streamName
            });
          },
          deploy: function (streamDefinition, properties) {
            $log.info('Deploy Stream ' + streamDefinition.name);
            return $resource($rootScope.xdAdminServerUrl + '/streams/deployments/' + streamDefinition.name, null, {
              deploy: {
                method: 'POST',
                params: {
                  properties: properties
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
          }
        };
      });
});
