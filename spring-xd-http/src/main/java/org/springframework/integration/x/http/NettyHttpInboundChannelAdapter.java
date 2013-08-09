/*
 * Copyright 2013 the original author or authors.
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

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.http.MediaType;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

/**
 * @author Mark Fisher
 */
public class NettyHttpInboundChannelAdapter extends MessageProducerSupport {

	private final int port;

	private volatile ServerBootstrap bootstrap;

	public NettyHttpInboundChannelAdapter(int port) {
		this.port = port;
	}

	@Override
	protected void doStart() {
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
			if (content.readable()) {
				Map<String, String> messageHeaders = new HashMap<String, String>();
				for (Entry<String, String> entry : request.getHeaders()) {
					if (!entry.getKey().toUpperCase().startsWith("ACCEPT")
							&& !entry.getKey().toUpperCase().equals("CONNECTION")) {
						messageHeaders.put(entry.getKey(), entry.getValue());
					}
					if (entry.getKey().equalsIgnoreCase("Content-Type")) {
						charsetToUse = MediaType.parseMediaType(entry.getValue()).getCharSet();
					}
				}
				// ISO-8859-1 is the default http charset when not set
				charsetToUse = charsetToUse == null ? Charset.forName("ISO-8859-1") : charsetToUse;
				messageHeaders.put("requestPath", request.getUri());
				messageHeaders.put("requestMethod", request.getMethod().toString());
				sendMessage(MessageBuilder.withPayload(content.toString(charsetToUse)).copyHeaders(messageHeaders)
						.build());
			}
			writeResponse(request, e.getChannel());
		}

		private void writeResponse(HttpRequest request, Channel channel) {
			boolean keepAlive = isKeepAlive(request);
			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
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
