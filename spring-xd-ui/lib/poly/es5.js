/** @license MIT License (c) copyright 2013 original authors */
/**
 * ES5 polyfills / shims for AMD loaders
 *
 * @author Brian Cavalier
 * @author John Hann
 */
(function (define) {
define(function (require) {

	var object = require('./object');
	var string = require('./string');
	var date = require('./date');
	require('./array');
	require('./function');
	require('./json');
	require('./xhr');

	return {
		failIfShimmed: object.failIfShimmed,
		setWhitespaceChars: string.setWhitespaceChars,
		setIsoCompatTest: date.setIsoCompatTest
	};

});
}(
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
