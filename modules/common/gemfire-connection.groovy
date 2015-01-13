beans {
	xmlns([gfe: 'http://www.springframework.org/schema/gemfire'])

	def useLocator = environment.activeProfiles.contains("use-locator")
	def hosts = environment.getProperty('host').replaceAll("\\s", "").split(',')
	def ports = environment.getProperty('port').replaceAll("\\s", "").split(',')

	gfe.'pool'(id: 'client-pool', 'subscription-enabled': '#{subscriptionEnabled}') {
		//expects a list of hosts and on port or a list of ports and one host or equal size lists of both. Validated by
		//Module Options Metadata class, but not for native REST calls.
		if (hosts.size() == 1 || ports.size() == 1) {
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

		else {
			for (int i = 0; i < hosts.size(); i++) {
				if (useLocator) {
					gfe.locator(host: "${hosts[i]}", port: "${ports[i]}")
				} else {
					gfe.server(host: "${hosts[i]}", port: "${ports[i]}")
				}
			}
		}
	}
}
