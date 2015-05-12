beans {
	xmlns([gfe: 'http://www.springframework.org/schema/gemfire'])

	subscriptionEnabled Boolean, false
	def xdhome = environment.getProperty('XD_HOME')
	importBeans "file:${xdhome}/modules/common/gemfire-connection.groovy"

	gfe.'client-cache'(id: 'client-cache', 'use-bean-factory-locator': false)
	gfe.'client-region'(id: 'region', 'cache-ref': 'client-cache', name: '${regionName}', 'data-policy': 'EMPTY')
}
