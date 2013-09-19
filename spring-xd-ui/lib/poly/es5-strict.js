/** @license MIT License (c) copyright 2013 original authors */
/**
 * stricter ES5 polyfills / shims for AMD loaders
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

	var failTestRx;

	failTestRx = /^define|^prevent|descriptor$/i;

	function regexpShouldThrow (feature) {
		return failTestRx.test(feature);
	}

	// set unshimmable Object methods to be somewhat strict:
	object.failIfShimmed(regexpShouldThrow);
	// set strict whitespace
	string.setWhitespaceChars('\\s');

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
