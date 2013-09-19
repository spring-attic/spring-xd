/** @license MIT License (c) copyright 2013 original authors */
/**
 * Object polyfill / shims
 *
 * @author Brian Cavalier
 * @author John Hann
 */
/**
 * The goal of these shims is to emulate a JavaScript 1.8.5+ environments as
 * much as possible.  While it's not feasible to fully shim Object,
 * we can try to maximize code compatibility with older js engines.
 *
 * Note: these shims cannot fix `for (var p in obj) {}`. Instead, use this:
 *     Object.keys(obj).forEach(function (p) {}); // shimmed Array
 *
 * Also, these shims can't prevent writing to object properties.
 *
 * If you want your code to fail loudly if a shim can't mimic ES5 closely
 * then set the AMD loader config option `failIfShimmed`.  Possible values
 * for `failIfShimmed` include:
 *
 * true: fail on every shimmed Object function
 * false: fail never
 * function: fail for shims whose name returns true from function (name) {}
 *
 * By default, no shims fail.
 *
 * The following functions are safely shimmed:
 * create (unless the second parameter is specified since that calls defineProperties)
 * keys
 * getOwnPropertyNames
 * getPrototypeOf
 * isExtensible
 *
 * In order to play nicely with several third-party libs (including Promises/A
 * implementations), the following functions don't fail by default even though
 * they can't be correctly shimmed:
 * freeze
 * seal
 * isFrozen
 * isSealed
 *
 * The poly/strict module will set failIfShimmed to fail for some shims.
 * See the documentation for more information.
 *
 * IE missing enum properties fixes copied from kangax:
 * https://github.com/kangax/protolicious/blob/master/experimental/object.for_in.js
 *
 * TODO: fix Object#propertyIsEnumerable for IE's non-enumerable props to match Object.keys()
 */
(function (define) {
define(function (require) {
"use strict";

	var base = require('./lib/_base');

	var refObj,
		refProto,
		has__proto__,
		hasNonEnumerableProps,
		getPrototypeOf,
		keys,
		featureMap,
		shims,
		secrets,
		protoSecretProp,
		hasOwnProp = 'hasOwnProperty',
		doc = typeof document != 'undefined' && document,
		testEl = doc && doc.createElement('p'),
		undef;

	refObj = Object;
	refProto = refObj.prototype;

	has__proto__ = typeof {}.__proto__ == 'object';

	hasNonEnumerableProps = (function () {
		for (var p in { valueOf: 1 }) return false;
		return true;
	}());

	// TODO: this still doesn't work for IE6-8 since object.constructor && object.constructor.prototype are clobbered/replaced when using `new` on a constructor that has a prototype. srsly.
	// devs will have to do the following if they want this to work in IE6-8:
	// Ctor.prototype.constructor = Ctor
	getPrototypeOf = has__proto__
		? function (object) { assertIsObject(object); return object.__proto__; }
		: function (object) {
			assertIsObject(object);
			// return null according to the investigation result at:
			// https://github.com/cujojs/poly/pull/21
			if (object == refProto) {
				return null;
			}
			return protoSecretProp && object[protoSecretProp](secrets)
				? object[protoSecretProp](secrets.proto)
				: object.constructor ? object.constructor.prototype : refProto;
		};

	keys = !hasNonEnumerableProps
		? _keys
		: (function (masked) {
			return function (object) {
				var result = _keys(object), i = 0, m;
				while (m = masked[i++]) {
					if (hasProp(object, m)) result.push(m);
				}
				return result;
			}
		}([ 'constructor', hasOwnProp, 'isPrototypeOf', 'propertyIsEnumerable', 'toString', 'toLocaleString', 'valueOf' ]));

	featureMap = {
		'object-create': 'create',
		'object-freeze': 'freeze',
		'object-isfrozen': 'isFrozen',
		'object-seal': 'seal',
		'object-issealed': 'isSealed',
		'object-getprototypeof': 'getPrototypeOf',
		'object-keys': 'keys',
		'object-getownpropertynames': 'getOwnPropertyNames',
		'object-isextensible': 'isExtensible',
		'object-preventextensions': 'preventExtensions',
		'object-defineproperty-obj': function () {
			return hasDefineProperty({});
		},
		'object-defineproperty-dom': function () {
			return doc && hasDefineProperty(testEl);
		},
		'object-defineproperties-obj': function () {
			return hasDefineProperties({});
		},
		'object-defineproperties-dom': function () {
			return doc && hasDefineProperties(testEl);
		},
		'object-getownpropertydescriptor-obj': function () {
			return hasGetOwnPropertyDescriptor({});
		},
		'object-getownpropertydescriptor-dom': function () {
			return doc && hasGetOwnPropertyDescriptor(testEl);
		}
	};

	shims = {};

	secrets = {
		proto: {}
	};

	protoSecretProp = !has('object-getprototypeof') && !has__proto__ && hasNonEnumerableProps && hasOwnProp;

	// we might create an owned property to hold the secrets, but make it look
	// like it's not an owned property.  (affects getOwnPropertyNames, too)
	if (protoSecretProp) (function (_hop) {
		refProto[hasOwnProp] = function (name) {
			if (name == protoSecretProp) return false;
			return _hop.call(this, name);
		};
	}(refProto[hasOwnProp]));

	if (!has('object-create')) {
		Object.create = shims.create = create;
	}

	if (!has('object-freeze')) {
		Object.freeze = shims.freeze = noop;
	}

	if (!has('object-isfrozen')) {
		Object.isFrozen = shims.isFrozen = nope;
	}

	if (!has('object-seal')) {
		Object.seal = shims.seal = noop;
	}

	if (!has('object-issealed')) {
		Object.isSealed = shims.isSealed = nope;
	}

	if (!has('object-getprototypeof')) {
		Object.getPrototypeOf = shims.getPrototypeOf = getPrototypeOf;
	}

	if (!has('object-keys')) {
		Object.keys = keys;
	}

	if (!has('object-getownpropertynames')) {
		Object.getOwnPropertyNames = shims.getOwnPropertyNames
			= getOwnPropertyNames;
	}

	if (!has('object-defineproperties-obj')) {
		// check if dom has it (IE8)
		Object.defineProperties = shims.defineProperties
			= has('object-defineproperties-dom')
				? useNativeForDom(Object.defineProperties, defineProperties)
				: defineProperties;
	}

	if (!has('object-defineproperty-obj')) {
		// check if dom has it (IE8)
		Object.defineProperty = shims.defineProperty
			= has('object-defineproperty-dom')
				? useNativeForDom(Object.defineProperty, defineProperty)
				: defineProperty;
	}

	if (!has('object-isextensible')) {
		Object.isExtensible = shims.isExtensible = isExtensible;
	}

	if (!has('object-preventextensions')) {
		Object.preventExtensions = shims.preventExtensions = preventExtensions;
	}

	if (!has('object-getownpropertydescriptor-obj')) {
		// check if dom has it (IE8)
		Object.getOwnPropertyDescriptor = shims.getOwnPropertyDescriptor
			= has('object-getownpropertydescriptor-dom')
				? useNativeForDom(Object.getOwnPropertyDescriptor, getOwnPropertyDescriptor)
				: getOwnPropertyDescriptor;
	}

	function hasDefineProperty (object) {
		if (('defineProperty' in Object)) {
			try {
				// test it
				Object.defineProperty(object, 'sentinel1', { value: 1 })
				return 'sentinel1' in object;
			}
			catch (ex) { /* squelch */ }
		}
	}

	// Note: MSDN docs say that IE8 has this function, but tests show
	// that it does not! JMH
	function hasDefineProperties (object) {
		if (('defineProperties' in Object)) {
			try {
				// test it
				Object.defineProperties(object, { 'sentinel2': { value: 1 } })
				return 'sentinel2' in object;
			}
			catch (ex) { /* squelch */ }
		}
	}

	function hasGetOwnPropertyDescriptor (object) {
		if (('getOwnPropertyDescriptor' in Object)) {
			object['sentinel3'] = true;
			try {
				return (Object.getOwnPropertyDescriptor(object, 'sentinel3').value);
			}
			catch (ex) { /* squelch */ }
		}
	}

	function PolyBase () {}

	// for better compression
	function hasProp (object, name) {
		return object.hasOwnProperty(name);
	}

	function _keys (object) {
		var result = [];
		for (var p in object) {
			if (hasProp(object, p)) {
				result.push(p);
			}
		}
		return result;
	}

	function create (proto, props) {
		var obj;

		if (typeof proto != 'object') throw new TypeError('prototype is not of type Object or Null.');

		PolyBase.prototype = proto;
		obj = new PolyBase();
		PolyBase.prototype = null;

		// provide a mechanism for retrieving the prototype in IE 6-8
		if (protoSecretProp) {
			var orig = obj[protoSecretProp];
			obj[protoSecretProp] = function (name) {
				if (name == secrets) return true; // yes, we're using secrets
				if (name == secrets.proto) return proto;
				return orig.call(this, name);
			};
		}

		if (arguments.length > 1) {
			// defineProperties could throw depending on `failIfShimmed`
			Object.defineProperties(obj, props);
		}

		return obj;
	}

	function defineProperties (object, descriptors) {
		var names, name;
		names = keys(descriptors);
		while ((name = names.pop())) {
			Object.defineProperty(object, name, descriptors[name]);
		}
		return object;
	}

	function defineProperty (object, name, descriptor) {
		object[name] = descriptor && descriptor.value;
		return object;
	}

	function getOwnPropertyDescriptor (object, name) {
		return hasProp(object, name)
			? {
				value: object[name],
				enumerable: true,
				configurable: true,
				writable: true
			}
			: undef;
	}

	function getOwnPropertyNames (object) {
		return keys(object);
	}

	function isExtensible (object) {
		var prop = '_poly_';
		try {
			// create unique property name
			while (prop in object) prop += '_';
			// try to set it
			object[prop] = 1;
			return hasProp(object, prop);
		}
		catch (ex) { return false; }
		finally {
			try { delete object[prop]; } catch (ex) { /* squelch */ }
		}
	}

	function preventExtensions (object) {
		return object;
	}

	function useNativeForDom (orig, shim) {
		return function (obj) {
			if (base.isElement(obj)) return orig.apply(this, arguments);
			else return shim.apply(this, arguments);
		};
	}

	function failIfShimmed (failTest) {
		var shouldThrow;

		if (typeof failTest == 'function') {
			shouldThrow = failTest;
		}
		else {
			// assume truthy/falsey
			shouldThrow = function () { return failTest; };
		}

		// create throwers for some features
		for (var feature in shims) {
			Object[feature] = shouldThrow(feature)
				? createFlameThrower(feature)
				: shims[feature];
		}
	}

	function assertIsObject (o) {
		if (typeof o != 'object') {
			throw new TypeError('Object.getPrototypeOf called on non-object');
		}
	}

	function createFlameThrower (feature) {
		return function () {
			throw new Error('poly/object: ' + feature + ' is not safely supported.');
		}
	}

	function has (feature) {
		var ret;
		var prop = featureMap[feature];

		if (base.isFunction(prop)) {
			// cache return value, ensure boolean
			ret = featureMap[feature] = !!prop(refObj);
		}
		else if (base.isString(prop)) {
			ret = featureMap[feature] = prop in refObj;
		}
		else {
			// return cached evaluate result
			ret = prop;
		}

		return ret;
	}

	function noop (it) { return it; }

	function nope (it) { return false; }

	return {
		failIfShimmed: failIfShimmed
	};

});
}(
	typeof define == 'function' && define.amd
		? define
		: function (factory) { module.exports = factory(require); }
));
