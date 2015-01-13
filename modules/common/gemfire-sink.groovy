beans {
	xmlns([gfe: 'http://www.springframework.org/schema/gemfire'])
	gfe.'client-cache'(id: 'client-cache', 'use-bean-factory-locator': false, close: false)
	gfe.'client-region'(id: 'region', 'cache-ref': 'client-cache', name: '${regionName}', 'data-policy': 'EMPTY')
	def useLocator = environment.activeProfiles.contains("use-locator")
	def hosts = environment.getProperty('host').replaceAll("\\s", "").split(',')
	def ports = environment.getProperty('port').replaceAll("\\s", "").split(',')

	gfe.'pool'(id: 'client-pool') {
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
}
