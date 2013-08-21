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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.dirt.stream.AlreadyDeployedException;
import org.springframework.xd.dirt.stream.DefinitionAlreadyExistsException;
import org.springframework.xd.dirt.stream.MissingRequiredDefinitionException;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.dsl.DSLException;

/**
 * Central class for behavior common to all REST controllers.
 * 
 * @author Eric Bottard
 */
@ControllerAdvice
public class RestControllerAdvice {

	private final Log logger = LogFactory.getLog(this.getClass());

	/*
	 * Note that any controller-specific exception handler is resolved first. So for example, having a
	 * onException(Exception e) resolver at a controller level will prevent the one from this class to be triggered.
	 */

	/**
	 * Handles the case where client submitted an ill valued request (missing parameter).
	 */
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public VndErrors onMissingServletRequestParameterException(MissingServletRequestParameterException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the general error case. Report server-side error.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public VndErrors onException(Exception e) {
		String logref = log(e);
		String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
		return new VndErrors(logref, msg);
	}

	/**
	 * Handles the case where client referenced an unknown entity.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchStreamException(NoSuchDefinitionException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client referenced an entity that already exists.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onStreamAlreadyExistsException(DefinitionAlreadyExistsException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client referenced an entity that already exists.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onMissingRequiredDefinitionException(MissingRequiredDefinitionException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client tried to deploy something that is already deployed.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onStreamAlreadyDeployedException(AlreadyDeployedException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client tried to deploy something that is has an invalid definition.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onInvalidDefintion(DSLException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchMetricException(NoSuchMetricException e) {
		String logref = log(e);
		return new VndErrors(logref, e.getMessage());
	}

	private String log(Throwable t) {
		logger.error("Caught exception while handling a request", t);
		// TODO: use a more semantically correct VndError 'logref'
		return t.getClass().getSimpleName();
	}
}
