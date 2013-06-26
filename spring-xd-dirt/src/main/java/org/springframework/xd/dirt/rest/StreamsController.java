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

package org.springframework.xd.dirt.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.VndErrors.VndError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.rest.client.domain.Stream;

/**
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/streams")
@ExposesResourceFor(Stream.class)
public class StreamsController {

	private StreamDeployer streamDeployer;

	@Autowired
	public StreamsController(StreamDeployer streamDeployer) {
		this.streamDeployer = streamDeployer;
	}

	/**
	 * Create a new Stream / Tap.
	 *
	 * @param name
	 *            the name of the stream to create (required)
	 * @param dsl
	 *            some representation of the stream behavior (required)
	 * @deprecated use POST on /streams instead
	 */
	@Deprecated
	@RequestMapping(value = "/{name}", method = { RequestMethod.PUT })
	@ResponseStatus(HttpStatus.CREATED)
	public void olddeploy(@PathVariable("name") String name,
			@RequestBody String dsl) {
		deploy(name, dsl);
	}

	/**
	 * Create a new Stream / Tap.
	 *
	 * @param name
	 *            the name of the stream to create (required)
	 * @param definition
	 *            some representation of the stream behavior, expressed in the
	 *            XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public Stream deploy(@RequestParam("name") String name,
			@RequestParam("definition") String definition) {
		streamDeployer.deployStream(name, definition);
		Stream result = new Stream(name);
		return result;
	}

	/**
	 * Request removal of an existing stream.
	 *
	 * @param name
	 *            the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		streamDeployer.undeployStream(name);
	}

	/**
	 * Handles the case where client submitted an ill valued request (most
	 * likely empty request body).
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public VndError onIllegalArgumentException(IllegalArgumentException iae) {
		String msg = StringUtils.hasText(iae.getMessage()) ? iae.getMessage()
				: "IllegalArgumentException";
		return new VndError("IllegalArgumentException", msg);
	}

	/**
	 * Handles the general error case. Report server-side error.
	 */
	@ResponseBody
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public VndError onException(Exception e) {
		String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e
				.getClass().getSimpleName();
		return new VndError(e.getClass().getSimpleName(), msg);
	}

}
