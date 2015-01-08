beans {
	foo String, 'foo'
	// This works as well
	// bar String, environment.getProperty('bar')
	bar String, '${bar}'
}

