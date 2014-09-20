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
define(['angular', 'xregexp', 'moment'], function(angular) {
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
    }])
    .directive('xdFormatStream', [function() {
      var mainRegex = new XRegExp('(--[\\p{Z}]*(password|passwd)[\\p{Z}]*=[\\p{Z}]*)([\\p{N}|\\p{L}|\\p{Po}]*)', 'gi');
      var subRegex = new XRegExp('\\P{C}', 'gi');
      var linkFunction = function(scope, element) {
        scope.$watch('xdFormatStream', function(originalStreamDefinition){
          if(originalStreamDefinition) {
            var result = XRegExp.replace(originalStreamDefinition, mainRegex, function(match, p1, p2, p3) {
              return p1 + XRegExp.replace(p3, subRegex,'*');
            });
            element.html(result);
          }
        });
      };
      return {
        restrict: 'A',
        scope: {
          xdFormatStream: '='
        },
        link: linkFunction,
      };
    }])
    .directive('xdDuration', [function() {

      var linkFunction = function(scope, el) {
        var startDateTime;
        var endDateTime;
        var element;

        function updateDuration() {
          if (startDateTime && endDateTime) {
            var duration = moment.duration(endDateTime - startDateTime);
            element.html(duration.asMilliseconds() + ' ms');
            console.log(duration);
          }
        }
        element = el;
        scope.$watch('start', function(value){
          if (value) {
            startDateTime = moment(value);
            updateDuration();
          }
        });
        scope.$watch('end', function(value){
          if (value) {
            endDateTime = moment(value);
            updateDuration();
          }
        });

      };
      return {
        restrict: 'A',
        scope: {
          xdDuration: '=',
          start: '=',
          end: '='
        },
        link: linkFunction,
      };
    }])
    .directive('xdDateTime', [function() {
      var dateTimeFormat = 'YYYY-MM-DD HH:mm:ss,SSS';

      var linkFunction = function(scope, element, attributes) {

        function formatDateTime(dateTimeValue) {
          if (dateTimeValue) {
            var startDateTime = moment(dateTimeValue);
            element.html('<span title="UTC Timezone offset: ' + moment().zone() +' minutes">' + startDateTime.format(dateTimeFormat) + '</span>');
          }
          else {
            element.html('N/A');
          }
        }

        formatDateTime(attributes.xdDateTime);

        attributes.$observe('xdDateTime', function(value){
          if (value) {
            formatDateTime(value);
          }
        });
      };
      return {
        restrict: 'A',
        scope: {
          xdDateTime: '@'
        },
        link: linkFunction,
      };
    }])
    .directive('integer', function() {
      var INTEGER_REGEXP = /^\-?\d+$/;
      return {
        require: 'ngModel',
        link: function(scope, element, attributes, controller) {
          controller.$parsers.unshift(function(viewValue) {
            if (INTEGER_REGEXP.test(viewValue)) {
              // it is valid
              controller.$setValidity('integer', true);
              return viewValue;
            } else {
              // it is invalid, return undefined (no model update)
              controller.$setValidity('integer', false);
              return undefined;
            }
          });
        }
      };
    })
    .directive('xdModal', function() {
      return {
        restrict: 'A',
        link: function(scope, element) {
          scope.closeModal = function() {
            element.modal('hide');
          };
        }
      };
    })
    .directive('xdDeploymentStatus', function() {
      var linkFunction = function(scope) {
        scope.$watch('xdDeploymentStatus', function(resource){
          if (resource) {
            if (resource.deleted) {
              scope.labelClass = 'danger';
              scope.label = 'Deleted';
            }
            else if (!resource.deleted && !resource.deployed) {
              scope.labelClass = 'warning';
              scope.label = 'Undeployed';
            }
          }
        });
      };
      return {
        restrict: 'A',
        scope: {
          xdDeploymentStatus: '='
        },
        link: linkFunction,
        templateUrl: 'scripts/directives/xdDeploymentStatus.html'
      };
    })
    .directive('xdPopover', function() {
      return {
        restrict: 'A',
        link: function(scope, element, attributes) {
          attributes.$observe('xdPopover', function(attributeValue){
            element.popover({
              placement: 'bottom',
              html: 'true',
              trigger: 'click',
              content: function () {
                return $(attributeValue).html();
              }
            })
            .on('show.bs.popover', function(){
              if (typeof scope.stopPolling === 'function') {
                scope.stopPolling();
              }
              $(this).data('bs.popover').tip().css('max-width', $(this).closest('#xd-content').width() + 'px');
              scope.$on('$destroy', function() {
                angular.element('.popover').remove();
              });
            })
            .on('hide.bs.popover', function(){
              if (typeof scope.startPolling === 'function') {
                scope.startPolling();
              }
            });
          });
        }
      };
    })
    .directive('xdTooltip', function() {
      return {
        restrict: 'A',
        link: function(scope, element, attributes) {
          attributes.$observe('title', function(){
            element.tooltip()
            .on('show.bs.tooltip', function(){
              if (typeof scope.stopPolling === 'function') {
                scope.stopPolling();
              }
            })
            .on('hide.bs.tooltip', function(){
              if (typeof scope.startPolling === 'function') {
                scope.startPolling();
              }
            });
          });
        }
      };
	})
    .directive('notTheSameAs', function() {
      return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attributes, controller) {
          var validate = function(viewValue) {
            var comparisonModel = attributes.notTheSameAs;

            if(!viewValue || !comparisonModel){
              controller.$setValidity('notTheSameAs', true);
            }
            controller.$setValidity('notTheSameAs', viewValue !== comparisonModel);
            return viewValue;
          };
          controller.$parsers.unshift(validate);
          controller.$formatters.push(validate);

          attributes.$observe('notTheSameAs', function(){
            return validate(controller.$viewValue);
          });
        }
      };
    });
});
