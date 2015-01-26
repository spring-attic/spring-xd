beans {
	xmlns([gfe   : 'http://www.springframework.org/schema/gemfire',
		   intgfe: 'http://www.springframework.org/schema/integration/gemfire'])

	def xdhome = environment.getProperty('XD_HOME')
	importBeans "file:${xdhome}/modules/common/gemfire-source.groovy"

	gfe.'cq-listener-container'(id: "cqContainer", cache: "client-cache")

	intgfe.'cq-inbound-channel-adapter'(query: '${query}', channel: 'route-on-data-type',
			'cq-listener-container': 'cqContainer', expression: 'newValue', durable: false)
}
