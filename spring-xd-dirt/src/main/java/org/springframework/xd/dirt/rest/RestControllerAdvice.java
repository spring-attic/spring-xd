/*
 * Copyright 2013-2015 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.dirt.cluster.ContainerShutdownException;
import org.springframework.xd.dirt.cluster.ModuleMessageRateNotFoundException;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.integration.bus.RabbitAdminException;
import org.springframework.xd.dirt.integration.bus.rabbit.NothingToDeleteException;
import org.springframework.xd.dirt.job.BatchJobAlreadyExistsException;
import org.springframework.xd.dirt.job.JobExecutionAlreadyRunningException;
import org.springframework.xd.dirt.job.JobExecutionNotRunningException;
import org.springframework.xd.dirt.job.JobInstanceAlreadyCompleteException;
import org.springframework.xd.dirt.job.JobParametersInvalidException;
import org.springframework.xd.dirt.job.JobRestartException;
import org.springframework.xd.dirt.job.NoSuchBatchJobException;
import org.springframework.xd.dirt.job.NoSuchBatchJobInstanceException;
import org.springframework.xd.dirt.job.NoSuchJobExecutionException;
import org.springframework.xd.dirt.job.NoSuchStepExecutionException;
import org.springframework.xd.dirt.module.ModuleAlreadyExistsException;
import org.springframework.xd.dirt.module.ModuleNotDeployedException;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.stream.AlreadyDeployedException;
import org.springframework.xd.dirt.stream.DefinitionAlreadyExistsException;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.NotDeployedException;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.dirt.util.PageNotFoundException;

/**
 * Central class for behavior common to all REST controllers.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@ControllerAdvice
public class RestControllerAdvice {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the general error case. Report server-side error.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public VndErrors onException(Exception e) {
		String logref = logError(e);
		String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
		return new VndErrors(logref, msg);
	}

	/**
	 * Handles the case where client referenced an unknown entity.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchDefinitionException(NoSuchDefinitionException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client referenced an entity that already exists.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onDefinitionAlreadyExistsException(DefinitionAlreadyExistsException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client tried to deploy something that is already deployed.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onAlreadyDeployedException(AlreadyDeployedException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client tried to un-deploy something that is not currently deployed.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onNotDeployedException(NotDeployedException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	/**
	 * Handles the case where client tried to deploy something that is has an invalid definition.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onInvalidDefinition(StreamDefinitionException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.CONFLICT)
	public VndErrors onModuleAlreadyExistsException(ModuleAlreadyExistsException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchMetricException(NoSuchMetricException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onModuleNotDeployedException(ModuleNotDeployedException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchModuleException(NoSuchModuleException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchJobExecutionException(NoSuchJobExecutionException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onJobExecutionNotRunningException(JobExecutionNotRunningException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchStepExecutionException(NoSuchStepExecutionException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	private String logDebug(Throwable t) {
		logger.debug("Caught exception while handling a request", t);
		// TODO: use a more semantically correct VndError 'logref'
		return t.getClass().getSimpleName();
	}

	private String logError(Throwable t) {
		logger.error("Caught exception while handling a request", t);
		// TODO: use a more semantically correct VndError 'logref'
		return t.getClass().getSimpleName();
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onJobExecutionAlreadyRunningException(JobExecutionAlreadyRunningException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onJobRestartException(JobRestartException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onJobInstanceAlreadyCompleteException(JobInstanceAlreadyCompleteException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchJobException(NoSuchBatchJobException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onBatchJobAlreadyExists(BatchJobAlreadyExistsException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchJobInstanceException(NoSuchBatchJobInstanceException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onContainerShutdownException(ContainerShutdownException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onJobParametersInvalidException(JobParametersInvalidException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchContainerException(NoSuchContainerException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onModuleMessageRateNotFoundException(ModuleMessageRateNotFoundException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndErrors onNoSuchPageException(PageNotFoundException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public VndErrors onNothingToDeleteException(NothingToDeleteException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onRabbitAdminException(RabbitAdminException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public VndErrors onBadCredentialsException(BadCredentialsException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}

	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public VndErrors onUnsatisfiedServletRequestParameterException(UnsatisfiedServletRequestParameterException e) {
		String logref = logDebug(e);
		return new VndErrors(logref, e.getMessage());
	}
}
