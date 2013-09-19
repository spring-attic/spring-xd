/** @license MIT License (c) copyright 2013 original authors */
/**
 * poly/strict
 *
 * @author Brian Cavalier
 * @author John Hann
 *
 * @deprecated Please use poly/es5-strict
 */
(function (define) {
define(function (require) {
"use strict";

	return require('./es5-strict');

});
}(
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
