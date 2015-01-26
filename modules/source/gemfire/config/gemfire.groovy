beans {
	xmlns([gfe   : 'http://www.springframework.org/schema/gemfire',
		   intgfe: 'http://www.springframework.org/schema/integration/gemfire',
		   si    : 'http://www.springframework.org/schema/integration'])

	def xdhome = environment.getProperty('XD_HOME')
	importBeans "file:${xdhome}/modules/common/gemfire-source.groovy"

	gfe.'client-region'(id: 'region', 'cache-ref': 'client-cache', name: '${regionName}', 'data-policy': 'EMPTY') {
		gfe.'key-interest' {
			bean(id: 'key', 'class': 'java.lang.String') {
				'constructor-arg'(value: 'ALL_KEYS')
			}
		}
	}

	si.channel(id: 'output')
	intgfe.'inbound-channel-adapter'(region: 'region', channel: 'route-on-data-type', expression: '${cacheEventExpression}')
}
