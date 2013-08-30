println("Groovy processing payload '" + payload +"'");
if (payload.contains('a')) {
	return ":foo"
}
else {
	return ":bar"
}