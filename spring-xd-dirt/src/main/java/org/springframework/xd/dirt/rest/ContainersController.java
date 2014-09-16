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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerShutdownException;
import org.springframework.xd.dirt.cluster.DetailedContainer;
import org.springframework.xd.dirt.cluster.ModuleMessageRateNotFoundException;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.DetailedContainerResource;

/**
 * Handles interaction with the runtime containers/and its modules.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@Controller
@RequestMapping("/cluster/containers")
@ExposesResourceFor(DetailedContainerResource.class)
public class ContainersController {

	@Autowired
	private ContainerRepository containerRepository;

	private ResourceAssemblerSupport<DetailedContainer, DetailedContainerResource> resourceAssembler =
			new RuntimeContainerResourceAssembler();

	private RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());

	@Value("${management.contextPath:/management}")
	private String managementContextPath;

	private final static String CONTAINER_HOST_URI_PROTOCOL = "http://";

	private final static String SHUTDOWN_ENDPOINT = "/shutdown";

	@Autowired
	public ContainersController(ContainerRepository containerRepository) {
		this.containerRepository = containerRepository;
	}

	/**
	 * List all the available containers along with the message rates for
	 * each deployed modules.
	 *
	 * @throws ModuleMessageRateNotFoundException
	 * @throws JSONException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<DetailedContainerResource> list(Pageable pageable,
			PagedResourcesAssembler<DetailedContainer> assembler) throws ModuleMessageRateNotFoundException,
			JSONException {
		Page<DetailedContainer> containers = containerRepository.findAllRuntimeContainers(pageable);
		for (DetailedContainer container : containers) {
			String containerHost = container.getAttributes().getIp();
			String containerManagementPort = container.getAttributes().getManagementPort();
			if (StringUtils.hasText(containerManagementPort)) {
				Map<String, HashMap<String, Double>> messageRates = new HashMap<String, HashMap<String, Double>>();
				for (ModuleMetadata moduleMetadata : container.getDeployedModules()) {
					String moduleName = moduleMetadata.getName();
					String moduleLabel = moduleName.substring(0, moduleName.indexOf('.'));
					String request = CONTAINER_HOST_URI_PROTOCOL + containerHost + ":"
							+ containerManagementPort + "/management/jolokia/read/xd." + moduleMetadata.getUnitName()
							+ ":module=" + moduleLabel + ".*,component=*,name=%s/MeanSendRate";
					try {
						HashMap<String, Double> rate = new HashMap<String, Double>();
						if (moduleMetadata.getModuleType().equals(ModuleType.source)) {
							rate.put("output", getMessageRate(String.format(request, "output")));
						}
						else if (moduleMetadata.getModuleType().equals(ModuleType.sink)) {
							rate.put("input", getMessageRate(String.format(request, "input")));
						}
						else if (moduleMetadata.getModuleType().equals(ModuleType.processor)) {
							rate.put("output", getMessageRate(String.format(request, "output")));
							rate.put("input", getMessageRate(String.format(request, "input")));
						}
						messageRates.put(moduleMetadata.getId(), rate);
					}
					catch (RestClientException e) {
						throw new ModuleMessageRateNotFoundException(e.getMessage());
					}
				}
				container.setMessageRates(messageRates);
			}
		}
		return assembler.toResource(containers, resourceAssembler);
	}

	/**
	 * Get the message rate for the given jolokia request URL.
	 *
	 * @param requestURL the request URL for message rate
	 * @return the message rate
	 * @throws JSONException
	 */
	private Double getMessageRate(String requestURL) throws JSONException {
		String response = restTemplate.getForObject(requestURL, String.class).toString();
		JSONObject jObject = new JSONObject(response);
		JSONObject value = jObject.getJSONObject("value");
		return (Double) value.getJSONObject((String) value.names().get(0)).get("MeanSendRate");
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
