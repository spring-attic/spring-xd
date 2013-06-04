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

package org.springframework.xd.dirt.stream;


/**
 * This is a temporary "server" for the REST API. Currently it only handles simple
 * stream configurations (tokens separated by pipes) without any parameters. This
 * will be completely replaced by a more robust solution. Intended for demo only.
 *
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 * @author Gary Russell
 */
public class DefaultStreamServer extends StreamServer {

	public DefaultStreamServer(StreamDeployer streamDeployer) {
		super(streamDeployer);
	}

}
