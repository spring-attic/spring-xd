beans {
	xmlns([gfe: 'http://www.springframework.org/schema/gemfire', si: 'http://www.springframework.org/schema/integration'])
	gfe.'client-cache'(id: 'client-cache', 'use-bean-factory-locator': false, close: false)

	subscriptionEnabled Boolean, true
	def xdhome = environment.getProperty('xd.home')
	importBeans "file:${xdhome}/modules/common/gemfire-connection.groovy"

	si.channel(id: 'output')
	si.'payload-type-router'('input-channel': 'route-on-data-type', 'default-output-channel': 'output') {
		si.mapping(type: 'com.gemstone.gemfire.pdx.PdxInstance', channel: 'convert')
	}

	si.transformer('input-channel': 'convert', 'output-channel': 'output', method: 'toString', ref: 'jsonStringToObjectTransformer')

	jsonStringToObjectTransformer org.springframework.integration.x.gemfire.JsonStringToObjectTransformer
}
