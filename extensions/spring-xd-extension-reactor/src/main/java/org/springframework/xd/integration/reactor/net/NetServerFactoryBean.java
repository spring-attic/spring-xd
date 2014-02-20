package org.springframework.xd.integration.reactor.net;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.SmartLifecycle;
import reactor.core.Environment;
import reactor.io.encoding.Codec;
import reactor.io.encoding.DelimitedCodec;
import reactor.io.encoding.LengthFieldCodec;
import reactor.io.encoding.StandardCodecs;
import reactor.net.NetServer;
import reactor.net.encoding.syslog.SyslogCodec;
import reactor.net.netty.tcp.NettyTcpServer;
import reactor.net.netty.udp.NettyDatagramServer;
import reactor.net.tcp.spec.TcpServerSpec;
import reactor.net.udp.spec.DatagramServerSpec;
import reactor.util.Assert;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jon Brisbin
 */
@SuppressWarnings("unchecked")
public class NetServerFactoryBean implements FactoryBean<NetServer>, SmartLifecycle {

	private final ReentrantLock startLock = new ReentrantLock();
	private final Environment env;
	private volatile boolean started = false;

	private int     phase       = 0;
	private boolean autoStartup = true;

	private Class<? extends NetServer> serverImpl;
	private NetServer                  server;
	private String                     dispatcher;

	private String host              = null;
	private int    port              = 3000;
	private Codec  codec             = StandardCodecs.BYTE_ARRAY_CODEC;
	private String framing           = "delimited";
	private String delimiter         = "LF";
	private int    lengthFieldLength = 4;
	private String transport         = "tcp";

	public NetServerFactoryBean(Environment env) {this.env = env;}

	public NetServerFactoryBean setDispatcher(String dispatcher) {
		this.dispatcher = dispatcher;
		return this;
	}

	public NetServerFactoryBean setPhase(int phase) {
		this.phase = phase;
		return this;
	}

	public NetServerFactoryBean setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		return this;
	}

	public NetServerFactoryBean setHost(String host) {
		Assert.notNull(host, "Host cannot be null.");
		this.host = host;
		return this;
	}

	public NetServerFactoryBean setPort(int port) {
		Assert.isTrue(port > 0, "Port must be greater than 0");
		this.port = port;
		return this;
	}

	public NetServerFactoryBean setCodec(String codec) {
		if("bytes".equals(codec)) {
			this.codec = StandardCodecs.BYTE_ARRAY_CODEC;
		} else if("string".equals(codec)) {
			this.codec = StandardCodecs.STRING_CODEC;
		} else if("syslog".equals(codec)) {
			this.codec = new SyslogCodec();
		} else {
			throw new IllegalArgumentException("Codec '" + codec + "' not recognized.");
		}
		return this;
	}

	public NetServerFactoryBean setFraming(String framing) {
		Assert.isTrue("delimited".equals(framing) || "length".equals(framing));
		this.framing = framing;
		return this;
	}

	public NetServerFactoryBean setLengthFieldLength(int lengthFieldLength) {
		this.lengthFieldLength = lengthFieldLength;
		return this;
	}

	public NetServerFactoryBean setTransport(String transport) {
		if("tcp".equals(transport)) {
			this.serverImpl = NettyTcpServer.class;
		} else if("udp".equals(transport)) {
			this.serverImpl = NettyDatagramServer.class;
		} else {
			throw new IllegalArgumentException("Transport must be either 'tcp' or 'udp'");
		}
		this.transport = transport;
		return this;
	}

	@Override
	public boolean isAutoStartup() {
		return autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		startLock.lock();
		try {
			server.shutdown();
			started = false;
		} finally {
			startLock.unlock();
			if(null != callback) {
				callback.run();
			}
		}
	}

	@Override
	public void start() {
		startLock.lock();
		try {
			server.start();
			started = true;
		} finally {
			startLock.unlock();
		}
	}

	@Override
	public void stop() {
		stop(null);
	}

	@Override
	public boolean isRunning() {
		startLock.lock();
		try {
			return started;
		} finally {
			startLock.unlock();
		}
	}

	@Override
	public int getPhase() {
		return phase;
	}

	@Override
	public NetServer getObject() throws Exception {
		if(null == server) {
			InetSocketAddress bindAddress;
			if(null == host) {
				bindAddress = InetSocketAddress.createUnresolved("0.0.0.0", port);
			} else {
				bindAddress = new InetSocketAddress(host, port);
			}

			Codec framedCodec = null;
			if("delimited".equals(framing)) {
				if("LF".equals(delimiter)) {
					framedCodec = new DelimitedCodec(this.codec);
				}
			} else if("length".equals(framing)) {
				framedCodec = new LengthFieldCodec(lengthFieldLength, this.codec);
			}
			if(null == framedCodec) {
				framedCodec = codec;
			}

			if("tcp".equals(transport)) {
				TcpServerSpec spec = new TcpServerSpec(serverImpl);
				spec.env(env);

				if(null != dispatcher) { spec.dispatcher(dispatcher); }

				server = (NetServer)spec.listen(bindAddress.getHostName(), bindAddress.getPort())
				                        .codec(framedCodec)
				                        .get();
			} else if("udp".equals(transport)) {
				DatagramServerSpec spec = new DatagramServerSpec(serverImpl);
				spec.env(env);

				if(null != dispatcher) { spec.dispatcher(dispatcher); }

				server = (NetServer)spec.listen(bindAddress.getHostName(), bindAddress.getPort())
				                        .codec(framedCodec)
				                        .get();
			}
		}
		return server;
	}

	@Override
	public Class<?> getObjectType() {
		return NetServer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
