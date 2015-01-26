beans {
	xmlns([intgfe:'http://www.springframework.org/schema/integration/gemfire',si:'http://www.springframework.org/schema/integration'])
	def xdhome = environment.getProperty('XD_HOME')
	importBeans "file:${xdhome}/modules/common/gemfire-sink.groovy"
	si.channel(id:'input')
	intgfe.'outbound-channel-adapter'(region:'region',channel:'input') {
		intgfe.'cache-entries' {
			intgfe.entry(key:'${keyExpression}',value:'payload')
		}
	}
}
