/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerShutdownException;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.cluster.RuntimeContainer;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.container.store.RuntimeContainerRepository;
import org.springframework.xd.rest.domain.RuntimeContainerResource;

/**
 * Handles interaction with the runtime containers/and its modules.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@Controller
@RequestMapping("/runtime/containers")
@ExposesResourceFor(RuntimeContainerResource.class)
public class RuntimeContainersController {

	@Autowired
	private RuntimeContainerRepository runtimeContainerRepository;

	@Autowired
	private ContainerRepository containerRepository;

	private ResourceAssemblerSupport<RuntimeContainer, RuntimeContainerResource> resourceAssembler =
			new RuntimeContainerResourceAssembler();

	@Value("${management.contextPath:/management}")
	private String managementContextPath;

	private final static String CONTAINER_HOST_URI_PROTOCOL = "http://";

	private final static String SHUTDOWN_ENDPOINT = "/shutdown";

	@Autowired
	public RuntimeContainersController(ContainerRepository containerRepository) {
		this.containerRepository = containerRepository;
	}

	/**
	 * List all the available containers
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<RuntimeContainerResource> list(Pageable pageable,
			PagedResourcesAssembler<RuntimeContainer> assembler) {
		return assembler.toResource(runtimeContainerRepository.findAllRuntimeContainers(pageable),
				resourceAssembler);
	}

	/**
	 * Shutdown container by the given containerId.
	 *
	 * @throws NoSuchContainerException
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE, params = "containerId")
	@ResponseStatus(HttpStatus.OK)
	public void shutdownContainer(String containerId) throws NoSuchContainerException, ContainerShutdownException {
		Container container = this.containerRepository.findOne(containerId);
		if (container != null) {
			String containerHost = container.getAttributes().getIp();
			String containerManagementPort = container.getAttributes().getManagementPort();
			RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
			try {
				restTemplate.postForObject(CONTAINER_HOST_URI_PROTOCOL + containerHost + ":"
						+ containerManagementPort + managementContextPath + SHUTDOWN_ENDPOINT, Object.class,
						Object.class);

			}
			catch (RestClientException e) {
				throw new ContainerShutdownException(e.getMessage());
			}
		}
		else {
			throw new NoSuchContainerException("Container could not be found with id " + containerId);
		}
	}
}
