/*
 * Copyright 2014 the original author or authors.
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
 * Handles user logins.
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', '$state', 'userService', 'XDUtils',
          function ($scope, $state, user, utils) {
          $scope.loginFormData = {};
          $scope.login = function() {
            user.isAuthenticated = true;
            user.username = $scope.loginFormData.name;
            utils.growl.success('user ' + user.username + ' logged in.');
            $state.go('home.jobs.tabs.definitions');
          };
        }];
});
