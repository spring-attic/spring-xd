package org.springframework.xd.integration.reactor.net;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import reactor.core.Environment;
import reactor.net.NetServer;
import reactor.spring.messaging.factory.net.NetServerFactoryBean;

/**
 * Inbound ChannelAdapter that uses Reactor's {@code NetServer} abstraction to provide high-speed TCP or UDP ingest.
 *
 * @author Jon Brisbin
 */
public class NetServerInboundChannelAdapter extends MessageProducerSupport {

	private final    NetServerFactoryBean netServerFactoryBean;
	private volatile NetServer            server;

	public NetServerInboundChannelAdapter(Environment env) {
		this.netServerFactoryBean = new NetServerFactoryBean(env);
	}

	/**
	 * Set the name of the {@link reactor.event.dispatch.Dispatcher} to use, which will be pulled from the current {@link
	 * reactor.core.Environment}.
	 *
	 * @param dispatcher
	 * 		dispatcher name
	 *
	 * @return {@literal this}
	 */
	public void setDispatcher(String dispatcher) {
		this.netServerFactoryBean.setDispatcher(dispatcher);
	}

	/**
	 * Set the phase in which this bean should start.
	 *
	 * @param phase
	 * 		the phase
	 *
	 * @return {@literal this}
	 */
	@Override
	public void setPhase(int phase) {
		super.setPhase(phase);
		this.netServerFactoryBean.setPhase(phase);
	}

	/**
	 * Set whether to perform auto startup.
	 *
	 * @param autoStartup
	 * 		{@code true} to enable auto startup, {@code false} otherwise
	 *
	 * @return {@literal this}
	 */
	@Override
	public void setAutoStartup(boolean autoStartup) {
		super.setAutoStartup(autoStartup);
		this.netServerFactoryBean.setAutoStartup(autoStartup);
	}

	/**
	 * Set the host to which this server will bind.
	 *
	 * @param host
	 * 		the host to bind to (defaults to {@code 0.0.0.0})
	 *
	 * @return {@literal this}
	 */
	public void setHost(String host) {
		this.netServerFactoryBean.setHost(host);
	}

	/**
	 * Set the port to which this server will bind.
	 *
	 * @param port
	 * 		the port to bind to (defaults to {@code 3000})
	 *
	 * @return {@literal this}
	 */
	public void setPort(int port) {
		this.netServerFactoryBean.setPort(port);
	}

	/**
	 * Set the {@link reactor.io.encoding.Codec} to use to managing encoding and decoding of the data.
	 * <p>
	 * The options for codecs currently are:
	 * <ul>
	 * <li>{@code bytes} - Use the standard byte array codec.</li>
	 * <li>{@code string} - Use the standard String codec.</li>
	 * <li>{@code syslog} - Use the standard Syslog codec.</li>
	 * </ul>
	 * </p>
	 *
	 * @param codec
	 * 		the codec
	 *
	 * @return {@literal this}
	 */
	public void setCodec(String codec) {
		this.netServerFactoryBean.setCodec(codec);
	}

	/**
	 * Set the type of framing to use.
	 * <p>
	 * The options for framing are:
	 * <ul>
	 * <li>{@code delimited} - Means use a delimited line codec (defaults to {@code LF}).</li>
	 * <li>{@code length} - Means use a length-field based codec where the initial bytes of a message are the length of
	 * the rest of the message.</li>
	 * </ul>
	 * </p>
	 *
	 * @param framing
	 * 		type of framing
	 *
	 * @return {@literal this}
	 */
	public void setFraming(String framing) {
		this.netServerFactoryBean.setFraming(framing);
	}

	/**
	 * Set the length of the length field if using length-field framing.
	 *
	 * @param lengthFieldLength
	 * 		{@code 2} for a {@code short}, {@code 4} for an {@code int} (the default), or {@code 8} for a {@code long}
	 *
	 * @return {@literal this}
	 */
	public void setLengthFieldLength(int lengthFieldLength) {
		this.netServerFactoryBean.setLengthFieldLength(lengthFieldLength);
	}

	/**
	 * Set the transport to use for this {@literal NetServer}.
	 * <p>
	 * Options for transport currently are:
	 * <ul>
	 * <li>{@code tcp} - Use the built-in Netty TCP support.</li>
	 * <li>{@code udp} - Use the built-in Netty UDP support.</li>
	 * </ul>
	 * </p>
	 *
	 * @param transport
	 * 		the transport to use
	 *
	 * @return {@literal this}
	 */
	public void setTransport(String transport) {
		this.netServerFactoryBean.setTransport(transport);
	}

	@Override
	public String getComponentType() {
		return "reactor:netserver-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		try {
			this.netServerFactoryBean.setMessageHandler(new MessageHandler() {
				@Override
				public void handleMessage(Message<?> msg) throws MessagingException {
					sendMessage(msg);
				}
			});
			this.server = netServerFactoryBean.getObject();
		} catch(Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	protected void doStart() {
		try {
			server.start().await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void doStop() {
		try {
			server.shutdown().await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
