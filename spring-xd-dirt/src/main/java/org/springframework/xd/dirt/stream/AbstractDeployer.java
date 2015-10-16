/*
 * Copyright 2011-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.job.dsl.ComposedJobUtil;
import org.springframework.xd.dirt.job.dsl.JobParser;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Abstract implementation of the @link {@link org.springframework.xd.dirt.core.ResourceDeployer} interface. It provides
 * the basic support for calling CrudRepository methods and sending deployment messages.
 *
 * @author Luke Taylor
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Andy Clement
 * @author David Turanski
 */
public abstract class AbstractDeployer<D extends BaseDefinition> implements ResourceDeployer<D>, DeploymentValidator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractDeployer.class);

	/**
	 * Pattern used for parsing a single deployment property key. Group 1 is the module name, Group 2 is the 
	 * deployment property name.
	 */
	private static final Pattern DEPLOYMENT_PROPERTY_PATTERN = Pattern.compile("module\\.([^\\.]+)\\.([^=]+)");

	private final PagingAndSortingRepository<D, String> repository;

	private final ZooKeeperConnection zkConnection;

	protected final XDParser parser;
	
	protected final JobParser composedJobParser;

	/**
	 * Used in exception messages as well as indication to the parser.
	 */
	protected final ParsingContext definitionKind;

	protected AbstractDeployer(ZooKeeperConnection zkConnection, PagingAndSortingRepository<D, String> repository,
			XDParser parser, ParsingContext parsingContext) {
		Assert.notNull(zkConnection, "ZooKeeper connection cannot be null");
		Assert.notNull(repository, "Repository cannot be null");
		Assert.notNull(parsingContext, "Entity type kind cannot be null");
		this.zkConnection = zkConnection;
		this.repository = repository;
		this.definitionKind = parsingContext;
		this.parser = parser;
		this.composedJobParser = new JobParser();
	}

	@Override
	public D save(D definition) {
		Assert.notNull(definition, "Definition may not be null");
		String name = definition.getName();
		String def = definition.getDefinition();
		validateBeforeSave(name, def);
		if(!ComposedJobUtil.isComposedJobDefinition(def)){
			List<ModuleDescriptor> moduleDescriptors =  
					parser.parse(name, def, definitionKind);
			// todo: the result of parse() should already have correct (polymorphic) definitions
			List<ModuleDefinition> moduleDefinitions = createModuleDefinitions(moduleDescriptors);
			if (!moduleDefinitions.isEmpty()) {
				definition.setModuleDefinitions(moduleDefinitions);
			}
		}
		D savedDefinition = repository.save(definition);
		return afterSave(savedDefinition);
	}


	@Override
	public void validateBeforeSave(String name, String definition) {
		Assert.hasText(name, "name cannot be blank or null");
		D definitionFromRepo = getDefinitionRepository().findOne(name);
		if (definitionFromRepo != null) {
			throwDefinitionAlreadyExistsException(definitionFromRepo);
		}
		Assert.hasText(definition, "definition cannot be blank or null");
		if (!ComposedJobUtil.isComposedJobDefinition(definition)){
			parser.parse(name, definition, definitionKind);
		} else {
			composedJobParser.parse(definition);
		}
	}

	/**
	 * Create a list of ModuleDefinitions given the results of parsing the definition.
	 *
	 * @param moduleDescriptors The list of ModuleDescriptors resulting from parsing the definition.
	 * @return a list of ModuleDefinitions
	 */
	protected List<ModuleDefinition> createModuleDefinitions(List<ModuleDescriptor> moduleDescriptors) {
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(moduleDescriptors.size());
		for (ModuleDescriptor moduleDescriptor : moduleDescriptors) {
			moduleDefinitions.add(moduleDescriptor.getModuleDefinition());
		}
		return moduleDefinitions;
	}

	/**
	 * Return the ZooKeeper connection.
	 *
	 * @return the ZooKeeper connection
	 */
	protected ZooKeeperConnection getZooKeeperConnection() {
		return zkConnection;
	}

	/**
	 * Callback method that subclasses may override to get a chance to act on newly saved definitions.
	 */
	protected D afterSave(D savedDefinition) {
		return savedDefinition;
	}

	protected void throwDefinitionAlreadyExistsException(D definition) {
		throw new DefinitionAlreadyExistsException(definition.getName(), String.format(
				"There is already a %s named '%%s'", definitionKind));
	}

	protected void throwNoSuchDefinitionException(String name) {
		throw new NoSuchDefinitionException(name,
				String.format("There is no %s definition named '%%s'", definitionKind));
	}

	protected void throwDefinitionNotDeployable(String name) {
		throw new NoSuchDefinitionException(name,
				String.format("The %s named '%%s' cannot be deployed", definitionKind));
	}

	protected void throwNoSuchDefinitionException(String name, String definitionKind) {
		throw new NoSuchDefinitionException(name,
				String.format("There is no %s definition named '%%s'", definitionKind));
	}

	protected void throwNotDeployedException(String name) {
		throw new NotDeployedException(name, String.format("The %s named '%%s' is not currently deployed",
				definitionKind));
	}

	protected void throwAlreadyDeployedException(String name) {
		throw new AlreadyDeployedException(name,
				String.format("The %s named '%%s' is already deployed", definitionKind));
	}

	@Override
	public D findOne(String name) {
		return repository.findOne(name);
	}

	@Override
	public Iterable<D> findAll() {
		return repository.findAll();
	}

	@Override
	public Page<D> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Override
	public void deleteAll() {
		for (D d : findAll()) {
			delete(d.getName());
		}
	}

	protected CrudRepository<D, String> getDefinitionRepository() {
		return repository;
	}

	/**
	 * Provides basic deployment behavior, whereby running state of deployed definitions is not persisted.
	 *
	 * @return the definition object for the given name
	 * @throws NoSuchDefinitionException if there is no definition by the given name
	 */
	protected D basicDeploy(String name, Map<String, String> properties) {
		Assert.hasText(name, "name cannot be blank or null");
		logger.trace("Deploying {}", name);

		final D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		validateDeploymentProperties(definition, properties);
		try {
			String deploymentPath = getDeploymentPath(definition);
			String statusPath = Paths.build(deploymentPath, Paths.STATUS);
			byte[] propertyBytes = DeploymentPropertiesFormat.formatDeploymentProperties(properties).getBytes("UTF-8");
			byte[] statusBytes = ZooKeeperUtils.mapToBytes(
					new DeploymentUnitStatus(DeploymentUnitStatus.State.deploying).toMap());

			zkConnection.getClient().inTransaction()
					.create().forPath(deploymentPath, propertyBytes).and()
					.create().withMode(CreateMode.EPHEMERAL).forPath(statusPath, statusBytes).and()
					.commit();
		}
		catch (KeeperException.NodeExistsException e) {
			throwAlreadyDeployedException(name);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return definition;
	}

	/**
	 * Validates that all deployment properties (of the form "module.<modulename>.<key>" do indeed
	 * reference module names that belong to the stream/job definition).
	 */
	private void validateDeploymentProperties(D definition, Map<String, String> properties) {
		List<ModuleDescriptor> modules = null;
		if(!ComposedJobUtil.isComposedJobDefinition(definition.getDefinition())){
			modules = parser.parse(definition.getName(), definition.getDefinition(), definitionKind);
		}
		else {
			modules = new ArrayList<ModuleDescriptor>();
			ModuleDescriptor.Builder builder =
					new ModuleDescriptor.Builder()
							.setType(ModuleType.job)
							.setGroup("job")
							.setModuleName(ComposedJobUtil.getComposedJobModuleName(definition.getName()))
							.setModuleLabel("")
							.setIndex(0);
			modules.add(builder.build());
		}
		Set<String> moduleLabels = new HashSet<String>(modules.size());
		for (ModuleDescriptor md : modules) {
			moduleLabels.add(md.getModuleLabel());
		}
		for (Map.Entry<String, String> pair : properties.entrySet()) {
			Matcher matcher = DEPLOYMENT_PROPERTY_PATTERN.matcher(pair.getKey());
			Assert.isTrue(matcher.matches(),
					String.format("'%s' does not match '%s'", pair.getKey(), DEPLOYMENT_PROPERTY_PATTERN));
			String moduleName = matcher.group(1);
			Assert.isTrue("*".equals(moduleName) || moduleLabels.contains(moduleName),
					String.format("'%s' refers to a module that is not in the list: %s", pair.getKey(), moduleLabels));
		}
	}

	protected abstract D createDefinition(String name, String definition);

	/**
	 * Return the ZooKeeper path used for deployment requests for the
	 * given definition.
	 *
	 * @param definition definition for which to obtain path
	 *
	 * @return ZooKeeper path for deployment requests
	 */
	protected abstract String getDeploymentPath(D definition);

	@Override
	public void validateBeforeDelete(String name) {
		D def = getDefinitionRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
	}

	@Override
	public void delete(String name) {
		D def = getDefinitionRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
		beforeDelete(def);
		getDefinitionRepository().delete(def);
	}

	/**
	 * Callback method that subclasses may override to get a chance to act on definitions that are about to be deleted.
	 */
	protected void beforeDelete(D definition) {
	}
}
