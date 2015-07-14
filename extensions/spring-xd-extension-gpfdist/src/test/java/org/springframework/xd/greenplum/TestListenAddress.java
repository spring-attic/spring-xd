/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.greenplum;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.springframework.xd.greenplum.gpfdist.GPFDistCodec;

import reactor.Environment;
import reactor.fn.Function;
import reactor.io.buffer.Buffer;
import reactor.io.net.NetStreams;
import reactor.io.net.Spec.HttpServerSpec;
import reactor.io.net.http.HttpServer;

public class TestListenAddress {

	@Test
	public void testBindZero() throws Exception {
		Environment.initializeIfEmpty().assignErrorJournal();

		HttpServer<Buffer, Buffer> httpServer = NetStreams
				.httpServer(new Function<HttpServerSpec<Buffer, Buffer>, HttpServerSpec<Buffer, Buffer>>() {

					@Override
					public HttpServerSpec<Buffer, Buffer> apply(HttpServerSpec<Buffer, Buffer> server) {
						return server
								.codec(new GPFDistCodec())
								.listen(0);
					}
				});
		httpServer.start().awaitSuccess();
		InetSocketAddress address = httpServer.getListenAddress();
		assertThat(address, notNullValue());
		assertThat(address.getPort(), not(0));
		httpServer.shutdown();
	}

}
