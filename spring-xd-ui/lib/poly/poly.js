/** @license MIT License (c) copyright 2013 original authors */
/**
 * polyfill / shim for AMD loaders
 *
 * @author Brian Cavalier
 * @author John Hann
 */
(function (define) {
define(function (require) {
"use strict";

	var all = require('./all');

	var poly = {};

	// copy all
	for (var p in all) poly[p] = all[p];

	return poly;

});
}(
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
