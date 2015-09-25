/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.StringTokenizer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.xd.dirt.cluster.ModuleMessageRateNotFoundException;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.container.store.DetailedContainer;
import org.springframework.xd.rest.domain.DetailedContainerResource;

/**
 * Handles interaction with the runtime containers/and its modules.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@Controller
@RequestMapping("/runtime/containers")
@ExposesResourceFor(DetailedContainerResource.class)
public class ContainersController {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ContainerRepository containerRepository;

	private ResourceAssemblerSupport<DetailedContainer, DetailedContainerResource> resourceAssembler = new RuntimeContainerResourceAssembler();

	private RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());

	@Value("${management.contextPath:/management}")
	private String managementContextPath;

	@Value("${xd.messageRateMonitoring.enabled:false}")
	private String enableMessageRates;

	private final static String CONTAINER_HOST_URI_PROTOCOL = "http://";

	private final static String SHUTDOWN_ENDPOINT = "/shutdown";

	private final static String JOLOKIA_XD_MODULE_MBEAN_URL = "/management/jolokia/read/xd.*:module=*,component=*,name=*";

	private final static String INPUT_CHANNEL_NAME = "input";

	private final static String OUTPUT_CHANNEL_NAME = "output";

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
			PagedResourcesAssembler<DetailedContainer> assembler) throws ModuleMessageRateNotFoundException {
		Page<DetailedContainer> containers = containerRepository.findAllRuntimeContainers(pageable, true);
		for (DetailedContainer container : containers) {
			if (!container.getDeployedModules().isEmpty()) {
				setMessageRates(container);
			}
		}
		return assembler.toResource(containers, resourceAssembler);
	}

	/**
	 * Set the message rates of all the deployed modules in the given container.
	 *
	 * @param container the container to set the message rates
	 */
	private void setMessageRates(DetailedContainer container) {
		String containerHost = container.getAttributes().getIp();
		String containerManagementPort = container.getAttributes().getManagementPort();
		if (StringUtils.hasText(containerManagementPort) && enableMessageRates.equalsIgnoreCase("true")) {
			Map<String, HashMap<String, Double>> messageRates = new HashMap<String, HashMap<String, Double>>();
			String request = String.format("%s%s:%s%s", CONTAINER_HOST_URI_PROTOCOL, containerHost,
					containerManagementPort, JOLOKIA_XD_MODULE_MBEAN_URL);
			try {
				String response = restTemplate.getForObject(request, String.class).toString();
				JSONObject jObject = new JSONObject(response);
				JSONObject value = jObject.getJSONObject("value");
				JSONArray jsonArray = value.names();
				// iterate over each module MBean
				for (int i = 0; i < jsonArray.length(); i++) {
					String mbeanKey = (String) jsonArray.get(i);
					StringTokenizer tokenizer = new StringTokenizer(mbeanKey, ",");
					if (mbeanKey.contains("component=MessageChannel")
							&& (mbeanKey.contains("name=" + INPUT_CHANNEL_NAME)
									|| mbeanKey.contains("name=" + OUTPUT_CHANNEL_NAME))) {
						while (tokenizer.hasMoreElements()) {
							String element = (String) tokenizer.nextElement();
							if (element.startsWith("module=")) {
								String key = String.format("%s", element.substring(element.indexOf("=") + 1));
								HashMap<String, Double> rate = (messageRates.get(key) != null) ? messageRates.get(key)
										: new HashMap<String, Double>();
								Double rateValue = (Double) value.getJSONObject((String) jsonArray.get(i)).get(
										"MeanSendRate");
								if (mbeanKey.contains("name=" + INPUT_CHANNEL_NAME)) {
									rate.put(INPUT_CHANNEL_NAME, rateValue);
								}
								else if (mbeanKey.contains("name=" + OUTPUT_CHANNEL_NAME)) {
									rate.put(OUTPUT_CHANNEL_NAME, rateValue);
								}
								messageRates.put(key, rate);
							}
						}
					}
				}
				container.setMessageRates(messageRates);
			}
			catch (RestClientException e) {
				logger.warn(String.format("Error getting message rate metrics for %s", container.getName()), e);
			}
			catch (JSONException jse) {
				logger.warn(String.format("Error getting message rate metrics for %s", container.getName()), jse);
			}
		}
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
