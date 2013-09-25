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

// tests for the xd backbone model
define(['xd.model'], function(model) {
	describe('model consistency test for non-batch artifacts', function() {

		beforeEach(function() {
			model.artifacts.forEach(function(artifact) {
				model.get(artifact.kind).reset();
			});
		});

		it('should have consistent artifact kinds', function() {
			model.artifacts.forEach(function(artifact) {
				expect(artifact.kind).toBeDefined();
				expect(artifact.name).toBeDefined();
				expect(artifact.url).toBeDefined();
			});
		});

		// for now, only test jobs artifacts.
		// since this is the only artifact currently enabled
		it('should have jobs artifact kind', function() {
			var artifact = model.findArtifact('jobs');
			expect(artifact).not.toBeNull();
			expect(artifact.kind).toBe('jobs');
			expect(artifact.name).toBeDefined('Jobs');
			expect(artifact.url).toBeDefined('jobs');
		});

		it('should have a jobs query', function() {
			var query = model.get('jobs');
			var artifact = query.get('artifact');
			expect(artifact.kind).toBe('jobs');
			expect(artifact.name).toBe('Jobs');
			expect(artifact.url).toBe('jobs');
		});
		it('jobs should have a correct url', function() {
			var query = model.get('jobs');
			var url = query.getUrl();
			expect(url).toBe('http://localhost:8088/jobs');
		});
		it('jobs should have correct pagination', function() {
			var query = model.get('jobs');
			var params = query.getHttpParams();
			expect(params.size).toBe(5);
			expect(params.page).toBe(0);

			query.set('number', 7);
			params = query.getHttpParams();
			expect(params.size).toBe(5);
			expect(params.page).toBe(7);

			query.set('number', 0);
			params = query.getHttpParams();
			expect(params.size).toBe(5);
			expect(params.page).toBe(0);
		});

		it('should be able to add results of a query', function() {

			// simulate the results of querying the server for jobs
			model.addQuery('jobs', {number:6}, [
				{name: 'foo', definition: 'blart'},
				{name: 'boo', definition: 'flart'}
			]);

			var query = model.get('jobs');
			var params = query.getHttpParams();
			expect(params.size).toBe(5);
			expect(params.page).toBe(6);
			var artifacts = query.get('artifacts');
			expect(artifacts.length).toBe(2);
			expect(artifacts.at(0).get('name')).toBe('foo');
			expect(artifacts.at(0).get('definition')).toBe('blart');
			expect(artifacts.at(1).get('name')).toBe('boo');
			expect(artifacts.at(1).get('definition')).toBe('flart');

			model.addQuery('jobs', {number:4}, [
				{name: 'foo1', definition: 'blart1'},
				{name: 'boo1', definition: 'flart1'},
				{name: 'coo1', definition: 'clart1'}
			]);

			query = model.get('jobs');
			params = query.getHttpParams();
			expect(params.size).toBe(5);
			expect(params.page).toBe(4);
			artifacts = query.get('artifacts');
			expect(artifacts.length).toBe(3);
			expect(artifacts.at(0).get('name')).toBe('foo1');
			expect(artifacts.at(0).get('definition')).toBe('blart1');
			expect(artifacts.at(1).get('name')).toBe('boo1');
			expect(artifacts.at(1).get('definition')).toBe('flart1');
			expect(artifacts.at(2).get('name')).toBe('coo1');
			expect(artifacts.at(2).get('definition')).toBe('clart1');
		});
	});

	describe('model consistency test for batch artifacts', function() {
		it('should correctly parse batch jobs', function() {
			var batchJobs = model.batchJobs;
			// TODO
		});
	});
});
