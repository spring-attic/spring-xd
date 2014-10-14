/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.x.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.logging.CommonsLoggerFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.internal.StringUtil;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author Marius Bogoevici
 */
public class NettyHttpInboundChannelAdapter extends MessageProducerSupport {

	private static Log logger = LogFactory.getLog(NettyHttpInboundChannelAdapter.class);

	/**
	 * Default max number of threads for the default {@link Executor}
	 */
	private static final int DEFAULT_CORE_POOL_SIZE = 16;

	/**
	 * Default max total size of queued events per channel for the default {@link Executor} (in bytes)
	 */
	private static final long DEFAULT_MAX_CHANNEL_MEMORY_SIZE = 1048576;

	/**
	 * Default max total size of queued events for the whole pool for the default {@link Executor} (in bytes)
	 */
	private static final long DEFAULT_MAX_TOTAL_MEMORY_SIZE = 1048576;

	private final int port;

	private final boolean ssl;

	private volatile ServerBootstrap bootstrap;

	private volatile ExecutionHandler executionHandler;

	private volatile Executor executor = new OrderedMemoryAwareThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE,
			DEFAULT_MAX_CHANNEL_MEMORY_SIZE, DEFAULT_MAX_TOTAL_MEMORY_SIZE);

	private boolean verboseLogging;

	static {
		// Use commons-logging for Netty logging
		InternalLoggerFactory.setDefaultFactory(new CommonsLoggerFactory());
	}

	/**
	 * Properties file containing keyStore=[resource], keyStore.passPhrase=[passPhrase]
	 */
	private volatile Resource sslPropertiesLocation;

	public NettyHttpInboundChannelAdapter(int port) {
		this(port, false);
	}

	public NettyHttpInboundChannelAdapter(int port, boolean ssl) {
		this.port = port;
		this.ssl = ssl;
	}

	/**
	 *
	 * @param executor The {@link Executor} to use with the Netty {@link ExecutionHandler} in the pipeline. This allows
	 *        any potential blocking operations done by message consumers to be removed from the I/O thread. The default
	 *        executor is an {@link OrderedMemoryAwareThreadPoolExecutor}, which is highly recommended because it
	 *        guarantees order of execution within a channel.
	 */
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "A non-null executor is required");
		this.executor = executor;
	}

	/**
	 * @param sslPropertiesLocation A properties resource containing a resource with key 'keyStore' and
	 * a pass phrase with key 'keyStore.passPhrase'.
	 */
	public void setSslPropertiesLocation(Resource sslPropertiesLocation) {
		this.sslPropertiesLocation = sslPropertiesLocation;
	}

	public void setVerboseLogging(boolean nettyLogging) {
		this.verboseLogging = nettyLogging;
	}

	@Override
	protected void doStart() {
		executionHandler = new ExecutionHandler(executor);
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setPipelineFactory(new PipelineFactory());
		bootstrap.bind(new InetSocketAddress(this.port));
	}

	@Override
	protected void doStop() {
		if (bootstrap != null) {
			bootstrap.shutdown();
		}
	}

	private void configureSSL(ChannelPipeline pipeline) {
		try {
			Assert.state(this.sslPropertiesLocation != null, "KeyStore and pass phrase properties file required");
			Properties sslProperties = new Properties();
			sslProperties.load(this.sslPropertiesLocation.getInputStream());
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String keyStoreName = sslProperties.getProperty("keyStore");
			Assert.state(StringUtils.hasText(keyStoreName), "keyStore property cannot be null");
			String keyStorePassPhrase = sslProperties.getProperty("keyStore.passPhrase");
			Assert.state(StringUtils.hasText(keyStorePassPhrase), "keyStore.passPhrase property cannot be null");
			Resource keyStore = resolver.getResource(keyStoreName);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(keyStore.getInputStream(), keyStorePassPhrase.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keyStorePassPhrase.toCharArray());
			sslContext.init(kmf.getKeyManagers(), null, null);
			SSLEngine engine = sslContext.createSSLEngine();
			engine.setUseClientMode(false);
			pipeline.addLast("ssl", new SslHandler(engine));
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to create SSLContext", e);
		}
	}

	private class PipelineFactory implements ChannelPipelineFactory {

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = new DefaultChannelPipeline();
			if (NettyHttpInboundChannelAdapter.this.ssl) {
				configureSSL(pipeline);
			}
			if (verboseLogging) {
				pipeline.addLast("logger", new LoggingHandler());
			}
			pipeline.addLast("decoder", new HttpRequestDecoder());
			pipeline.addLast("aggregator", new HttpChunkAggregator(1024 * 1024));
			pipeline.addLast("encoder", new HttpResponseEncoder());
			pipeline.addLast("compressor", new HttpContentCompressor());
			pipeline.addLast("executionHandler", executionHandler);
			pipeline.addLast("handler", new Handler());
			return pipeline;
		}

	}

	private class Handler extends SimpleChannelUpstreamHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if (logger.isDebugEnabled()) {
				logger.debug("Received HTTP request:\n" + indent(e.getMessage().toString()));
			}
			HttpRequest request = (HttpRequest) e.getMessage();
			ChannelBuffer content = request.getContent();
			Charset charsetToUse = null;
			boolean binary = false;
			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
			if (content.readable()) {
				Map<String, String> messageHeaders = new HashMap<String, String>();
				for (Entry<String, String> entry : request.getHeaders()) {
					if (entry.getKey().equalsIgnoreCase("Content-Type")) {
						MediaType contentType = MediaType.parseMediaType(entry.getValue());
						charsetToUse = contentType.getCharSet();
						messageHeaders.put(MessageHeaders.CONTENT_TYPE, entry.getValue());
						binary = MediaType.APPLICATION_OCTET_STREAM.equals(contentType);
					}
					else if (!entry.getKey().toUpperCase().startsWith("ACCEPT")
							&& !entry.getKey().toUpperCase().equals("CONNECTION")) {
						messageHeaders.put(entry.getKey(), entry.getValue());
					}
				}
				messageHeaders.put("requestPath", request.getUri());
				messageHeaders.put("requestMethod", request.getMethod().toString());
				try {
					AbstractIntegrationMessageBuilder<?> builder;
					if (binary) {
						builder = getMessageBuilderFactory().withPayload(content.array());
					}
					else {
						// ISO-8859-1 is the default http charset when not set
						charsetToUse = charsetToUse == null ? Charset.forName("ISO-8859-1") : charsetToUse;
						builder = getMessageBuilderFactory().withPayload(content.toString(charsetToUse));
					}
					builder.copyHeaders(messageHeaders);
					Message<?> message = builder.build();
					if (logger.isDebugEnabled()) {
						logger.debug("Sending message: " + message);
					}
					sendMessage(message);
				}
				catch (Exception ex) {
					logger.error("Error sending message", ex);
					response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
				}
			}
			writeResponse(request, response, e.getChannel());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			throw new RuntimeException(e.getCause());
		}

		private void writeResponse(HttpRequest request, HttpResponse response, Channel channel) {
			boolean keepAlive = isKeepAlive(request);
			if (keepAlive) {
				response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
				response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Sending HTTP response:\n" + indent(response.toString()));
			}
			ChannelFuture future = channel.write(response);
			if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	/**
	 * Indents the content of a multi-line string - used mainly for allowing the pretty display of Netty {@code toString()} output/
	 */
	private static String indent(String s) {
		return "\t"+ s.replace(StringUtil.NEWLINE, StringUtil.NEWLINE + "\t");
	}

}
