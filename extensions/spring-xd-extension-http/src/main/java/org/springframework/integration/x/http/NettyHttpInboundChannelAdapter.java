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

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

import org.springframework.http.MediaType;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;


/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 */
public class NettyHttpInboundChannelAdapter extends MessageProducerSupport {

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

	private volatile ServerBootstrap bootstrap;

	private volatile ExecutionHandler executionHandler;

	private volatile Executor executor = new OrderedMemoryAwareThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE,
			DEFAULT_MAX_CHANNEL_MEMORY_SIZE, DEFAULT_MAX_TOTAL_MEMORY_SIZE);

	public NettyHttpInboundChannelAdapter(int port) {
		this.port = port;
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

	private class PipelineFactory implements ChannelPipelineFactory {

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = new DefaultChannelPipeline();
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
					sendMessage(builder.build());
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
			ChannelFuture future = channel.write(response);
			if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

}
