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

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.stream.AbstractDeployer;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;

/**
 * Base Class for XD Controllers.
 *
 * @param <D> the kind of definition entity this controller deals with
 * @param <V> a resource assembler that knows how to build Ts out of Ds
 * @param <T> the resource class for D
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */

public abstract class XDController<D extends BaseDefinition, V extends ResourceAssemblerSupport<D, T>, T extends ResourceSupport> {

	private ResourceDeployer<D> deployer;

	private V resourceAssemblerSupport;

	protected XDController(AbstractDeployer<D> deployer, V resourceAssemblerSupport) {
		this.deployer = deployer;
		this.resourceAssemblerSupport = resourceAssemblerSupport;
	}

	protected ResourceDeployer<D> getDeployer() {
		return deployer;
	}

	/**
	 * Request removal of an existing module.
	 *
	 * @param name the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		deployer.delete(name);
	}

	/**
	 * Request removal of all modules.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() {
		deployer.deleteAll();
	}

	/**
	 * Request un-deployment of an existing named module.
	 *
	 * @param name the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		deployer.undeploy(name);
	}

	/**
	 * Request un-deployment of all modules.
	 */
	@RequestMapping(value = "_deployments", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() {
		deployer.undeployAll();
	}

	/**
	 * Request deployment of an existing named module.
	 *
	 * @param name the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		deployer.deploy(name);
	}

	/**
	 * Request deployment of all modules.
	 */
	@RequestMapping(value = "_deployments", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	public void deployAll() {
		deployer.deployAll();
	}

	/**
	 * Retrieve information about a single {@link ResourceSupport}.
	 *
	 * @param name the name of an existing tap (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ResourceSupport display(@PathVariable("name") String name) {
		final D definition = deployer.findOne(name);
		if (definition == null) {
			throw new NoSuchDefinitionException(name, "There is no definition named '%s'");
		}
		return resourceAssemblerSupport.toResource(definition);
	}

	/**
	 * List module definitions.
	 */
	// protected and not annotated with @RequestMapping due to the way
	// PagedResourcesAssemblerArgumentResolver works
	// subclasses should override and make public (or delegate)
	protected PagedResources<T> listValues(Pageable pageable, PagedResourcesAssembler<D> assembler) {
		Page<D> page = deployer.findAll(pageable);
		if (page.hasContent()) {
			return assembler.toResource(page, resourceAssemblerSupport);
		}
		else {
			return new PagedResources<T>(new ArrayList<T>(), null);
		}
	}

	/**
	 * Create a new Module.
	 *
	 * @param name The name of the module to create (required)
	 * @param definition The module definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public T save(@RequestParam("name") String name, @RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) {
		final D moduleDefinition = createDefinition(name, definition);
		final D savedModuleDefinition = deployer.save(moduleDefinition);
		if (deploy) {
			deployer.deploy(name);
		}
		final T result = resourceAssemblerSupport.toResource(savedModuleDefinition);
		return result;
	}

	protected abstract D createDefinition(String name, String definition);

}
