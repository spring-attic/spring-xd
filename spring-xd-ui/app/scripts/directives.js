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
 * Definition of custom directives.
 *
 * @author Gunnar Hillert
 */
define(['angular'], function(angular) {
  'use strict';
  angular.module('xdAdmin.directives', [])
    .directive('xdParseUrls', [function() {
      var urlPattern = /(http|ftp|https):\/\/[\w-]+(\.[\w-]+)+([\w.,@?^=%&amp;:\/~+#-]*[\w@?^=%&amp;\/~+#-])?/gi;

      return {
        restrict: 'A',
        //transclude: true,
        //scope: {},
        link: function (scope, element) {
          var originalValue = scope.contextValue.value;
          var newHtml = originalValue;
          var matches;
          
          if (originalValue.substring) {
            matches = originalValue.match(urlPattern);
          }
          if (typeof matches !== 'undefined') {
            angular.forEach(matches, function(url) {
              newHtml = newHtml.replace(url, '<a href=\''+ url + '\'>' + url + '</a>');
            });
          }
          element.html(newHtml);
        }
      };
    }]);
});