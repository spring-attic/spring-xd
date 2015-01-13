beans {
	xmlns([gfe: 'http://www.springframework.org/schema/gemfire', si: 'http://www.springframework.org/schema/integration'])
	gfe.'client-cache'(id: 'client-cache', 'use-bean-factory-locator': false, close: false)

	def useLocator = environment.activeProfiles.contains("use-locator")

	def hosts = environment.getProperty('host').replaceAll("\\s", "").split(',')
	def ports = environment.getProperty('port').replaceAll("\\s", "").split(',')

	gfe.'pool'(id: 'client-pool', 'subscription-enabled': true) {
		//expects a list of hosts and on port or a list of ports and one host or equal size lists of both
		hosts.each { h ->
			ports.each { p ->
				if (useLocator) {
					gfe.locator(host: "${h}", port: "${p}")
				} else {
					gfe.server(host: "${h}", port: "${p}")
				}
			}
		}
	}

	si.channel(id: 'output')
	si.'payload-type-router'('input-channel': 'route-on-data-type', 'default-output-channel': 'output') {
		si.mapping(type: 'com.gemstone.gemfire.pdx.PdxInstance', channel: 'convert')
	}

	si.transformer('input-channel': 'convert', 'output-channel': 'output', method: 'toString', ref: 'jsonStringToObjectTransformer')

	jsonStringToObjectTransformer org.springframework.integration.x.gemfire.JsonStringToObjectTransformer
}
