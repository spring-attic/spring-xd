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
/*jslint browser:true */
/*globale define describe it waits runs expect waitsFor beforeEach*/
/**
 * @author Andrew Eisenberg
 */

// tests for the xd backbone views
define(['xd.model', 'xd.viewer', 'views/navbar'], 
function(model, viewer, NavBar) {
	describe('navbar view', function() {
        it('should correcly render the navbar', function() {
            var navbar = new NavBar();
            var $navbar = navbar.render().$el;
            // should have a jobs tab and a dashboard tab, but 
            // other tabs are not active
            // they are specified using anchor tags
            var $a = $navbar.find('a');
            expect($a.length).toBe(4);
            expect($a[2].href).toBe(location.toString() + '#xd-dashboard');
            expect($a[3].href).toBe(location.toString() + '#xd-create-job');
        });
	});

	describe('artifacts-list-item view', function() {
		// TODO test sumpin
	});
	
	describe('batch view', function() {
		// TODO test sumpin
	});
	
	describe('batch details view', function() {
		// TODO test sumpin
	});
});