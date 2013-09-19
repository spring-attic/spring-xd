/** @license MIT License (c) copyright 2013 original authors */
/**
 * Function polyfill / shims
 *
 * @author Brian Cavalier
 * @author John Hann
 */
(function (define) {
define(function (require) {
"use strict";

	var base = require('./lib/_base');

	var bind,
		slice = [].slice,
		proto = Function.prototype,
		featureMap;

	featureMap = {
		'function-bind': 'bind'
	};

	function has (feature) {
		var prop = featureMap[feature];
		return base.isFunction(proto[prop]);
	}

	// check for missing features
	if (!has('function-bind')) {
		// adapted from Mozilla Developer Network example at
		// https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/bind
		bind = function bind (obj) {
			var args = slice.call(arguments, 1),
				self = this,
				nop = function () {},
				bound = function () {
				  return self.apply(this instanceof nop ? this : (obj || {}), args.concat(slice.call(arguments)));
				};
			nop.prototype = this.prototype || {}; // Firefox cries sometimes if prototype is undefined
			bound.prototype = new nop();
			return bound;
		};
		proto.bind = bind;
	}

	return {};

});
}(
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
