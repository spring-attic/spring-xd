/** @license MIT License (c) copyright 2013 original authors */
/**
 * XHR polyfill / shims
 *
 * @author Brian Cavalier
 * @author John Hann
 */
(function (global, define) {
define(function (require) {
"use strict";

	var progIds;

	// find XHR implementation
	if (typeof XMLHttpRequest == 'undefined') {
		// create xhr impl that will fail if called.
		assignCtor(function () { throw new Error("poly/xhr: XMLHttpRequest not available"); });
		// keep trying progIds until we find the correct one,
		progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];
		while (progIds.length && tryProgId(progIds.shift())) {}
	}

	function assignCtor (ctor) {
		// assign global.XMLHttpRequest function
		global.XMLHttpRequest = ctor;
	}

	function tryProgId (progId) {
		try {
			new ActiveXObject(progId);
			assignCtor(function () { return new ActiveXObject(progId); });
			return true;
		}
		catch (ex) {}
	}

});
}(
	typeof global != 'undefined' && global || this.global || this,
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
