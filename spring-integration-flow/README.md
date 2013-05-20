Spring Integration Flow
========================

#Goals
Spring Integration components already support common enterprise integration patterns. Sometimes it is desirable to create common message processing modules  which may be be parameterized and composed or reused in Spring Integration applications. 

The XD DIRT runtime is designed to support composition of integration modules into _streams_. Flows conform to the DIRT module architecture, but may also be used in traditional Spring Integration applications without requiring a runtime platform. 

# Usage

The application instantiates a flow referencing an existing module:

	<int-flow:flow id="subflow1"/>

The above bean definition instantiates a flow defined as "subflow1". The flow id references an existing module in the application's classpath located in /META-INF/spring/integration/module/subflow1.xml
 
The _flow_ element may be configured with property values required by the module, a list of active environment profiles, and additional bean definitions.

The module must contain a channel named _input_ and optionally one or more output channels matching the name _output_ or _output.*_    

##Properties

The following configurations are all supported:

Properties reference:	
	
	<util:properties id="myprops">
    	<prop key="key1>val1</prop>
	</util:properties>

	<int-flow:flow id="subflow1" properties="myprops"/>

Nested properties:
	
	<int-flow:flow id="subflow1">
    	<props>
        	<prop key="key1>val1</prop>
       </props>
	</int-flow:flow>

Using Spring's p namespace:
		
	
	<int-flow:flow id="subflow1" p:key1="val1"/>


It is possible to define multiple instances of the same flow with different configurations: 

	<int-flow:flow id="flow1" module-name="subflow1" p:key1="val1"/>
	<int-flow:flow id="flow2" module-name="subflow1" p:key1="val2"/>
    

By default the module name references the id attribute. 

##Profiles

If the module is configured for alternate profiles, you may declare a list of active profiles:

    <int-flow:flow id="flow1" module-name="subflow1" p:key1="val1" active-profiles="p1"/>
    
## Referenced Beans

The module may reference one or more beans that are defined externally. You may register such beans using the _additional_component_locations_ attribute:

    <int-flow:flow id="flow1" additional_component_locations="classpath:/META-INF/spring/myBeans.xml, file:/myDir/moreBeans.xml"/>


##Invoking a Flow

When a flow is declared, the module is created in a separate application context to support encapsulation and multiple configurations of the same module definition. Spring will automatically register channels in the main application context and bind them to the module:

       <int-flow:flow id="simple"/>

The above configuration will create channels _simple.input_ and _simple.output_ which are used to access the module. These channels may be referenced normally in the application, for example:

      <int:transformer input-channel="input" output-channel="simple.input" .../>
      <int:transformer input-channel="simple.output" output-channel="output" .../>
 
Note that the flow input channel is a DirectChannel and the output channel is a PublishSubscribe channel. 

An outbound gateway is also provided:

	<int-flow:outbound-gateway flow="flow1" request-channel="inputChannel1" reply-channel="outputChannel1"/>
  

A message sent on the gateway's input-channel is delegated to the flow. The message on the output-channel is a response from one of the flow's output channels. In the case of multiple output channels, the output channel name is identified in the header 

    FlowConstants.FLOW_OUTPUT_CHANNEL_HEADER = "flow.output.channel"


Flows may also be used in a chain just as any AbstractReplyProducingMessageHandler:

	<chain input-channel="inputChannel" output-channel="outputChannel">
    	<int-flow:outbound-gateway flow="flow1"/>
    	<int-flow:outbound-gateway flow="flow2"/> 
	</chain>

You may also declare an error channel on the gateway:

    <int-flow:outbound-gateway flow="flow1" request-channel="inputChannel1" reply-channel="outputChannel1" error-channel="errorChannel"/>

 
# Implementing a Flow Module
The flow element is used to locate the flow's spring bean definition file(s) by convention classpath:META-INF/spring/integration/module/${module-name}/*.xml. 

Along with any additional component locations, the configuration will create a separate application context. The module can be any valid Spring Integration message flow defining an _input_ channel and zero or more output channels matching the pattern _output_ or _output.*_. Here are some examples

Simple echo module:

    <int:bridge input-channel="input" output-channel="output"/>
    <int:channel id="output"/><int:bridge input-channel="input" output-channel="output"/>
    <int:channel id="output"/>
    
Using environment profiles:

    <int:channel id="output" />

	<beans profile="p1">
		<int:header-enricher input-channel="input"
			output-channel="output">
			<int:header name="profile" value="p1"/>
		</int:header-enricher>
	</beans>
	<beans profile="p2">
		<int:header-enricher input-channel="input"
			output-channel="output">
			<int:header name="profile" value="p2"/>
		</int:header-enricher>
	</beans>

Multiple outputs:

    <int:router input-channel="input" expression="'output.'+ payload"/>

	<int:channel id="output.foo" />
	<int:channel id="output.bar" />


Nested:

    <int-flow:flow id="simple" />

	<int-flow:outbound-gateway flow="simple"
		request-channel="input" reply-channel="output" />

	<int:channel id="output" />



Flow Outbound Gateway Internals
------------------------------
The outbound gateway is backed by FlowExecutingMessageHandler used to delegate messages to the configured flow and return any response. In order to share a module instance among multiple gateways, the handler subscribes to the flow output Publish-Subscribe channel and provides a correlation ID which it verifies to provide the correct reply message (if there is a reply).
