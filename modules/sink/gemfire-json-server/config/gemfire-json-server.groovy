beans {
	xmlns([intgfe:'http://www.springframework.org/schema/integration/gemfire',si:'http://www.springframework.org/schema/integration'])
	def xdhome = environment.getProperty('XD_HOME')
	importBeans "file:${xdhome}/modules/common/gemfire-sink.groovy"

	si.channel(id:'input')
	si.transformer('input-channel':'input', 'output-channel':'to.gemfire', method:'toObject', ref:'jsonToObjectTransformer')
	intgfe.'outbound-channel-adapter'(region:'region',id:'to.gemfire') {
		intgfe.'cache-entries' {
			intgfe.entry(key:'${keyExpression}',value:'payload')
		}
	}

	jsonToObjectTransformer org.springframework.integration.x.gemfire.JsonStringToObjectTransformer
}
