/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.server.admin.deployment.zk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.DeploymentUnit;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentPropertiesProvider;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentStatus;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


/**
 * Abstract ModuleRedeployer that deploy/re-deploys the unallocated/orphaned modules.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public abstract class ModuleRedeployer {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(ModuleRedeployer.class);

	/**
	 * The ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Repository from which to obtain containers in the cluster.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Container matcher for matching modules to containers.
	 */
	private final ContainerMatcher containerMatcher;

	/**
	 * Utility for writing module deployment requests to ZooKeeper.
	 */
	private final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Cache of children under the module deployment requests path.
	 */
	protected final PathChildrenCache moduleDeploymentRequests;

	/**
	 * Stream factory.
	 */
	protected final StreamFactory streamFactory;

	/**
	 * Job factory.
	 */
	protected final JobFactory jobFactory;

	/**
	 * State calculator for stream/job state.
	 */
	private final DeploymentUnitStateCalculator stateCalculator;

	/**
	 * Constructs {@code ModuleRedeployer}
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param containerRepository the repository to find the containers
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param moduleDeploymentRequests cache of children for requested module deployments path
	 * @param containerMatcher matches modules to containers
	 * @param moduleDeploymentWriter utility that writes deployment requests to zk path
	 * @param stateCalculator calculator for stream/job state
	 */
	public ModuleRedeployer(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository, StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			ModuleDeploymentWriter moduleDeploymentWriter, DeploymentUnitStateCalculator stateCalculator) {
		this.zkConnection = zkConnection;
		this.containerRepository = containerRepository;
		this.containerMatcher = containerMatcher;
		this.moduleDeploymentWriter = moduleDeploymentWriter;
		this.moduleDeploymentRequests = moduleDeploymentRequests;
		this.streamFactory = streamFactory;
		this.jobFactory = jobFactory;
		this.stateCalculator = stateCalculator;
	}
	/**
	 * Deploy unallocated/orphaned modules.
	 *
	 * @param container the container to deploy
	 * @throws Exception
	 */
	protected abstract void deployModules(Container container) throws Exception;

	/**
	 * Return the Curator framework client.
	 *
	 * @return Curator client
	 */
	protected CuratorFramework getClient() {
		return zkConnection.getClient();
	}

	/**
	 * Determine which containers, if any, have deployed a module for a stream.
	 *
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty
	 * list is returned if no containers have deployed it
	 *
	 * @throws Exception thrown by Curator
	 */
	private List<String> getContainersForStreamModule(ModuleDescriptor descriptor)
			throws Exception {
		List<String> containers = new ArrayList<String>();
		String moduleType = descriptor.getModuleDefinition().getType().toString();
		String moduleLabel = descriptor.getModuleLabel();
		String moduleDeploymentPath = Paths.build(Paths.STREAM_DEPLOYMENTS, descriptor.getGroup(), Paths.MODULES);
		try {
			List<String> moduleDeployments = getClient().getChildren().forPath(moduleDeploymentPath);
			for (String moduleDeployment : moduleDeployments) {
				StreamDeploymentsPath path = new StreamDeploymentsPath(
						Paths.build(moduleDeploymentPath, moduleDeployment));
				if (path.getModuleType().equals(moduleType)
						&& path.getModuleLabel().equals(moduleLabel)) {
					containers.add(path.getContainer());
				}
			}
		}
		catch (KeeperException.NoNodeException e) {
			// stream has not been (or is no longer) deployed
		}
		return containers;
	}

	/**
	 * Determine which containers, if any, have deployed job module of the given job.
	 *
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty
	 * list is returned if no containers have deployed it
	 *
	 * @throws Exception thrown by Curator
	 */
	private List<String> getContainersForJobModule(ModuleDescriptor descriptor)
			throws Exception {
		List<String> containers = new ArrayList<String>();
		//String moduleType = descriptor.getModuleDefinition().getType().toString();
		String moduleLabel = descriptor.getModuleLabel();
		String moduleDeploymentPath = Paths.build(Paths.JOB_DEPLOYMENTS, descriptor.getGroup(), Paths.MODULES);
		try {
			List<String> moduleDeployments = getClient().getChildren().forPath(moduleDeploymentPath);
			for (String moduleDeployment : moduleDeployments) {
				JobDeploymentsPath path = new JobDeploymentsPath(
						Paths.build(moduleDeploymentPath, moduleDeployment));
				if (path.getModuleLabel().equals(moduleLabel)) {
					containers.add(path.getContainer());
				}
			}
		}
		catch (KeeperException.NoNodeException e) {
			// job has not been (or is no longer) deployed
		}
		return containers;
	}

	/**
	 * Logger unwanted re-deployment of the module if the module count is less
	 * than or equal to zero.
	 * @param criteria the criteria for the module deployment
	 * @param moduleLabel the module label
	 */
	protected void logUnwantedRedeployment(String criteria, String moduleLabel) {
		StringBuilder builder = new StringBuilder();
		builder.append("Module '").append(moduleLabel).append("' is targeted to all containers");
		if (StringUtils.hasText(criteria)) {
			builder.append(" matching criteria '").append(criteria).append('\'');
		}
		builder.append("; it does not need to be redeployed");
		logger.info(builder.toString());
	}

	/**
	 * Redeploy a module to a container. This redeployment will occur if:
	 * <ul>
	 * 		<li>the module needs to be redeployed per the deployment properties</li>
	 * 		<li>the stream has not been destroyed</li>
	 * 		<li>the stream has not been undeployed</li>
	 * 		<li>there is a container that can deploy the stream module</li>
	 * </ul>
	 *
	 * @param moduleDeployment contains module redeployment details such as
	 *                         stream, module descriptor, and deployment properties
	 * @param arriving         if true, this module redeployment is being triggered
	 *                         by an arriving container; if false this is being
	 *                         triggered by a departing container
	 * @throws InterruptedException
	 */
	protected void redeployModule(ModuleDeployment moduleDeployment, boolean arriving) throws Exception {
		DeploymentUnit deploymentUnit = moduleDeployment.deploymentUnit;
		ModuleDescriptor moduleDescriptor = moduleDeployment.moduleDescriptor;
		RuntimeModuleDeploymentProperties deploymentProperties = moduleDeployment.runtimeDeploymentProperties;
		ModuleDeploymentStatus deploymentStatus = null;

		// in the case of a departing container, the module should
		// only be redeployed if count > 0
		if (arriving || deploymentProperties.getCount() > 0) {
			try {
				Collection<String> containers = (moduleDescriptor.getType() == ModuleType.job)
						? getContainersForJobModule(moduleDescriptor)
						: getContainersForStreamModule(moduleDescriptor);
				deploymentStatus = deployModule(moduleDeployment, containerMatcher, containers);
			}
			catch (NoContainerException e) {
				logger.warn("No containers available for redeployment of {} for stream {}",
						moduleDescriptor.getModuleLabel(),
						deploymentUnit.getName());
			}
			finally {
				updateDeploymentUnitState(moduleDeployment, deploymentStatus);
			}
		}
		else {
			logUnwantedRedeployment(deploymentProperties.getCriteria(), moduleDescriptor.getModuleLabel());
		}
	}

	/**
	 * Issue a module deployment request for the provided module using
	 * the provided container matcher. This also transitions the deployment
	 * unit state to {@link DeploymentUnitStatus.State#deploying}. Once
	 * the deployment attempt completes, the status for the deployment unit
	 * should be updated via {@link #updateDeploymentUnitState} using the
	 * {@link ModuleDeploymentStatus} returned from this method.
	 *
	 * @param moduleDeployment   contains module redeployment details such as
	 *                           stream, module descriptor, and deployment properties
	 * @param containerMatcher   matches modules to containers
	 * @param exclusions         containers to exclude from deployment since they are
	 *                           already hosting the module
	 * @return result of module deployment request
	 * @throws Exception
	 * @see #transitionToDeploying
	 */
	private ModuleDeploymentStatus deployModule(ModuleDeployment moduleDeployment,
			ContainerMatcher containerMatcher, Collection<String> exclusions) throws Exception {
		transitionToDeploying(moduleDeployment.deploymentUnit);
		
		Iterable<Container> containers = containerRepository.findAll();
		MatchingPredicate matchingPredicate = new MatchingPredicate(exclusions);
		Collection<Container> matchedContainers = containerMatcher.match(moduleDeployment.moduleDescriptor,
				moduleDeployment.runtimeDeploymentProperties, Iterables.filter(containers, matchingPredicate));
		if (matchedContainers.isEmpty()) {
			throw new NoContainerException();
		}
		return moduleDeploymentWriter.writeDeployment(moduleDeployment.moduleDescriptor,
				moduleDeployment.runtimeDeploymentProperties, matchedContainers.iterator().next());
	}

	/**
	 * Transitions the deployment unit state to {@link DeploymentUnitStatus.State#deploying}.
	 * This transition should occur before making a module deployment attempt.
	 *
	 * @param deploymentUnit deployment unit that contains a module to be deployed
	 * @throws Exception
	 * @see #updateDeploymentUnitState
	 */
	private void transitionToDeploying(DeploymentUnit deploymentUnit) throws Exception {
		String pathPrefix = (deploymentUnit instanceof Stream)
				? Paths.STREAM_DEPLOYMENTS
				: Paths.JOB_DEPLOYMENTS;

		getClient().setData().forPath(
				Paths.build(pathPrefix, deploymentUnit.getName(), Paths.STATUS),
				ZooKeeperUtils.mapToBytes(new DeploymentUnitStatus(
						DeploymentUnitStatus.State.deploying).toMap()));
	}

	/**
	 * Return a <em>mutable</em> collection of {@link ModuleDeploymentStatus module statuses}
	 * for all of the modules that comprise the provided {@link DeploymentUnit}. This
	 * information is obtained from ZooKeeper via the ephemeral nodes created
	 * by the individual containers that have deployed these modules.
	 * <p />
	 * This collection is used (and modified) in {@link #updateDeploymentUnitState}.
	 *
	 * @param deploymentUnit  deployment unit for which to return the individual
	 *                        module statuses
	 * @return mutable collection of status objects
	 * @throws Exception
	 * @see #updateDeploymentUnitState
	 */
	private Collection<ModuleDeploymentStatus> aggregateState(DeploymentUnit deploymentUnit) throws Exception {
		Assert.state(deploymentUnit instanceof Stream || deploymentUnit instanceof Job);
		String pathPrefix = (deploymentUnit instanceof Stream)
				? Paths.STREAM_DEPLOYMENTS
				: Paths.JOB_DEPLOYMENTS;

		String path = Paths.build(pathPrefix, deploymentUnit.getName(), Paths.MODULES);
		List<String> modules = getClient().getChildren().forPath(path);
		Collection<ModuleDeploymentStatus> results = new ArrayList<ModuleDeploymentStatus>();
		for (String module : modules) {
			String deploymentUnitName;
			ModuleType type;
			String label;
			int moduleSequence;
			String container;

			if (deploymentUnit instanceof Stream) {
				StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(Paths.build(path, module));
				deploymentUnitName = streamDeploymentsPath.getStreamName();
				Assert.state(deploymentUnitName.equals(deploymentUnit.getName()));
				type = ModuleType.valueOf(streamDeploymentsPath.getModuleType());
				label = streamDeploymentsPath.getModuleLabel();
				moduleSequence = streamDeploymentsPath.getModuleSequence();
				container = streamDeploymentsPath.getContainer();
			}
			else {
				JobDeploymentsPath jobDeploymentsPath = new JobDeploymentsPath(Paths.build(path, module));
				deploymentUnitName = jobDeploymentsPath.getJobName();
				Assert.state(deploymentUnitName.equals(deploymentUnit.getName()));
				type = ModuleType.job;
				label = jobDeploymentsPath.getModuleLabel();
				moduleSequence = jobDeploymentsPath.getModuleSequence();
				container = jobDeploymentsPath.getContainer();
			}

			ModuleDescriptor.Key moduleDescriptorKey = new ModuleDescriptor.Key(deploymentUnitName, type, label);
			results.add(new ModuleDeploymentStatus(container, moduleSequence, moduleDescriptorKey,
					ModuleDeploymentStatus.State.deployed, null));
		}

		return results;
	}

	/**
	 * Update the ZooKeeper ephemeral node that indicates the status for the
	 * provided deployment unit.
	 *
	 * @param moduleDeployment  module that a redeploy was attempted for
	 * @param deploymentStatus  deployment status for the module; may be null
	 *                          if a module deployment was not attempted
	 *                          (for example if there were no containers
	 *                          available for deployment)
	 * @throws Exception
	 */
	protected void updateDeploymentUnitState(ModuleDeployment moduleDeployment,
			ModuleDeploymentStatus deploymentStatus) throws Exception {
		final DeploymentUnit deploymentUnit = moduleDeployment.deploymentUnit;
		final ModuleDescriptor moduleDescriptor = moduleDeployment.moduleDescriptor;

		Collection<ModuleDeploymentStatus> aggregateStatuses = aggregateState(deploymentUnit);

		// If the module deployment was successful, it will be present in this
		// list; remove it to avoid duplication since the deployment status
		// that was returned from the deployment request will be used instead.
		// This is especially important if the deployment failed because
		// that deployment status will contain the error condition that
		// caused the failure.
		if (deploymentStatus != null) {
			for (Iterator<ModuleDeploymentStatus> iterator = aggregateStatuses.iterator(); iterator.hasNext();) {
				ModuleDeploymentStatus status = iterator.next();
				if (logger.isTraceEnabled()) {
					logger.trace("module deployment status: {}", status);
					logger.trace("deploymentStatus: {}", deploymentStatus);
				}

				if (status.getKey().getLabel().equals(moduleDescriptor.getModuleLabel())
						&& status.getContainer().equals(deploymentStatus.getContainer())) {
					iterator.remove();
				}
			}
			aggregateStatuses.add(deploymentStatus);
		}

		Assert.state(deploymentUnit instanceof Stream || deploymentUnit instanceof Job);
		boolean isStream = (deploymentUnit instanceof Stream);

		ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> provider = new DefaultModuleDeploymentPropertiesProvider(
				deploymentUnit);

		DeploymentUnitStatus status = stateCalculator.calculate(deploymentUnit, provider, aggregateStatuses);

		logger.info("Deployment state for {} '{}': {}",
				isStream ? "stream" : "job", deploymentUnit.getName(), status);

		getClient().setData().forPath(
				Paths.build(isStream ? Paths.STREAM_DEPLOYMENTS : Paths.JOB_DEPLOYMENTS,
						deploymentUnit.getName(), Paths.STATUS),
				ZooKeeperUtils.mapToBytes(status.toMap()));
	}

	/**
	 * Get all the module deployment requests.
	 *
	 * @return the list of all the requested modules' paths.
	 */
	protected List<ModuleDeploymentRequestsPath> getAllModuleDeploymentRequests() {
		List<ModuleDeploymentRequestsPath> requestedModulesPaths = new ArrayList<ModuleDeploymentRequestsPath>();
		for (ChildData requestedModulesData : moduleDeploymentRequests.getCurrentData()) {
			requestedModulesPaths.add(new ModuleDeploymentRequestsPath(requestedModulesData.getPath()));
		}
		return requestedModulesPaths;
	}


	/**
	 * Holds information about a module that needs to be re-deployed.
	 * The {@link Comparable} implementation for this class sorts
	 * first by deployment unit name followed by (reverse) module descriptor
	 * index - i.e., the biggest index appearing first.
	 *
	 * @author Eric Bottard
	 * @author Ilayaperumal Gopinathan
	 */
	protected static class ModuleDeployment implements Comparable<ModuleDeployment> {

		protected final DeploymentUnit deploymentUnit;

		protected final ModuleDescriptor moduleDescriptor;

		protected final RuntimeModuleDeploymentProperties runtimeDeploymentProperties;

		ModuleDeployment(DeploymentUnit deploymentUnit, ModuleDescriptor moduleDescriptor,
				RuntimeModuleDeploymentProperties runtimeDeploymentProperties) {
			this.deploymentUnit = deploymentUnit;
			this.moduleDescriptor = moduleDescriptor;
			this.runtimeDeploymentProperties = runtimeDeploymentProperties;
		}

		@Override
		public int compareTo(ModuleDeployment o) {
			int c = this.deploymentUnit.getName().compareTo(o.deploymentUnit.getName());
			if (c == 0) {
				c = o.moduleDescriptor.getIndex() - this.moduleDescriptor.getIndex();
			}
			return c;
		}

	}


	/**
	 * Predicate used to determine if a container should be returned by the ContainerMatcher.
	 */
	private class MatchingPredicate implements Predicate<Container> {

		/**
		 * Set of container names that should <b>not</b> be returned by {@link #apply(org.springframework.xd.dirt.cluster.Container)}.
		 */
		private final Set<String> exclusions;

		/**
		 * Create a MatchingPredicate with the provided containers as exclusions
		 *
		 * @param exclusions collection of names of containers to exclude
		 */
		private MatchingPredicate(Collection<String> exclusions) {
			Assert.notNull(exclusions);
			this.exclusions = Collections.unmodifiableSet(new HashSet<String>(exclusions));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean apply(Container input) {
			return input != null && (!exclusions.contains(input.getName()));
		}
	}
}
