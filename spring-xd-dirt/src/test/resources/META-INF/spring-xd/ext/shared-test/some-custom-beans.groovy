import org.springframework.integration.x.bus.LocalMessageBus
import org.springframework.xd.extensions.test.StubCodec
beans {
	codec(StubCodec)

	//This definition is only required because the LocalMessageBus does not inject a codec by default
	messageBus(LocalMessageBus) { codec = ref('codec') }
}
