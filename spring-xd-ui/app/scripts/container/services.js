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
 * XD Container services.
 *
 * @author Ilayaperumal Gopinathan
 */
define(['angular'], function (angular) {
  'use strict';

  return angular.module('xdContainerAdmin.services', [])
      .factory('RuntimeContainerService', function ($resource, $rootScope) {
        return {
          getRuntimeContainers: function () {
            return $resource($rootScope.xdAdminServerUrl + '/runtime/containers.json', {}, {
              getRuntimeContainers: {
                method: 'GET'
              }
            }).getRuntimeContainers();
          },
          getMessageRate: function (runtimeContainer, deployedModule) {
            var ip = runtimeContainer.attributes.ip;
            var mgmtPort = runtimeContainer.attributes.managementPort;
            var moduleName = deployedModule.name.split('.')[0];
            var unitName = deployedModule.unitName;
            var address = window.location.protocol + '//' + ip + ':' + mgmtPort +
                '/management/jolokia/read/xd.' + unitName + '%3Amodule=' + moduleName +
                '.*,component=*,name=*/MeanSendRate';
            return $resource(address, {}, {
              getMessageRate: {
                method: 'GET'
              }
            }).getMessageRate();
          },
          shutdownContainer: function (containerId) {
            return $resource($rootScope.xdAdminServerUrl + '/runtime/containers?containerId=' + containerId, null, {
              shutdownContainer: {
                method: 'DELETE'
              }
            }).shutdownContainer();
          }
        };
      });
});
