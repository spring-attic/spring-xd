package org.springframework.xd.integration.reactor.syslog;

import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import reactor.core.Environment;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.encoding.syslog.SyslogCodec;
import reactor.tcp.encoding.syslog.SyslogMessage;
import reactor.tcp.netty.NettyTcpServer;
import reactor.tcp.spec.TcpServerSpec;

/**
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapter extends MessageProducerSupport {

	private final TcpServer<SyslogMessage, Void> server;

	public SyslogInboundChannelAdapter(Environment env, int port) {
		this.server = new TcpServerSpec<SyslogMessage, Void>(NettyTcpServer.class)
				.env(env)
				.dispatcher(new SynchronousDispatcher())
				.codec(new SyslogCodec())
				.listen(port)
				.consume(new Consumer<TcpConnection<SyslogMessage, Void>>() {
					@Override
					public void accept(TcpConnection<SyslogMessage, Void> conn) {
						conn.in().consume(new Consumer<SyslogMessage>() {
							@Override
							public void accept(SyslogMessage syslogMsg) {
								Message<SyslogMessage> siMsg = MessageBuilder.withPayload(syslogMsg).build();
								sendMessage(siMsg);
							}
						});
					}
				})
				.get();
	}

	@Override
	public String getComponentType() {
		return "reactor:syslog-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
	}

	@Override
	protected void doStart() {
		server.start();
	}

	@Override
	protected void doStop() {
		server.shutdown();
	}

}
