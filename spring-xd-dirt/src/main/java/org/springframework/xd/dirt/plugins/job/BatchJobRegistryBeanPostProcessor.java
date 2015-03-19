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

package org.springframework.xd.dirt.plugins.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.xml.JobParserJobFactoryBean;
import org.springframework.batch.core.configuration.xml.StepParserStepFactoryBean;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.plugins.job.support.listener.XDJobListenerConstants;


/**
 * JobRegistryBeanPostProcessor that processes batch job from the job module.
 *
 * @author Ilayaperumal Gopinathan
 * @author Michael Minella
 */
public class BatchJobRegistryBeanPostProcessor extends JobRegistryBeanPostProcessor implements BeanFactoryAware,
		XDJobListenerConstants {

	private static final Logger logger = LoggerFactory.getLogger(BatchJobRegistryBeanPostProcessor.class);

	public static final String JOB = "job";

	private JobRegistry jobRegistry;

	private DefaultListableBeanFactory beanFactory;

	private DistributedJobLocator jobLocator;

	private String groupName;

	private List<JobExecutionListener> jobExecutionListeners = new ArrayList<JobExecutionListener>();

	private List<StepListener> stepListeners = new ArrayList<StepListener>();

	@Override
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		super.setJobRegistry(jobRegistry);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof DefaultListableBeanFactory) {
			this.beanFactory = (DefaultListableBeanFactory) beanFactory;
		}
		super.setBeanFactory(beanFactory);
	}

	public void setJobLocator(DistributedJobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	@Override
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof StepParserStepFactoryBean<?, ?>) {
			addStepListeners();
			if (!stepListeners.isEmpty()) {
				// Add the step listeners to the step parser factory bean
				((StepParserStepFactoryBean) bean).setListeners(this.stepListeners.toArray(new StepListener[this
						.stepListeners.size()]));
			}
		}
		else if (bean instanceof Job) {
			if (!jobRegistry.getJobNames().contains(groupName)) {
				String[] beansOfType = beanFactory.getBeanNamesForType(Job.class);

				if (beansOfType.length > 1) {
					if (beanName.equalsIgnoreCase(JOB)) {
						postProcessJob(bean, beanName);
					}
					else {
						logger.debug(beanName + " was not registered as a job since the context has more than one job " +
								"defined and it's id is not 'job'");
					}
				}
				else {
					postProcessJob(bean, beanName);
				}
			}
			else if (jobRegistry.getJobNames().contains(groupName)) {
				throw new BatchJobAlreadyExistsInRegistryException(groupName);
			}
		}
		return bean;
	}

	private void postProcessJob(Object bean, String beanName) {
		AbstractJob job = (AbstractJob) bean;
		job.setName(this.groupName);
		addJobExecutionListener();
		if (!this.jobExecutionListeners.isEmpty()) {
			// Add the job execution listeners to the job parser factory bean
			job.setJobExecutionListeners(this.jobExecutionListeners.toArray(new
					JobExecutionListener[this.jobExecutionListeners.size()]));
		}
		// Add the job name, job parameters incrementer and job restartable flag to {@link DistributedJobLocator}
		// Since, the Spring batch doesn't have persistent JobRegistry, the {@link DistributedJobLocator}
		// acts as the store to have jobName , job parameter incrementer and restartable flag to be
		// used by {@link DistributedJobService}
		jobLocator.addJob(this.groupName, (job.getJobParametersIncrementer() != null) ? true : false,
				job.isRestartable());
		jobLocator.addStepNames(this.groupName, job.getStepNames());
		super.postProcessAfterInitialization(bean, beanName);
	}

	private void addJobExecutionListener() {
		// Add all job execution listeners available in the bean factory
		// We won't have multiple batch job definitions on a given job module; hence all the job execution listeners
		// available in the bean factory correspond to the job module's batch job.
		Map<String, JobExecutionListener> listeners = this.beanFactory.getBeansOfType(JobExecutionListener.class);
		this.jobExecutionListeners.addAll(listeners.values());
	}

	private void addStepListeners() {
		if (this.beanFactory.containsBean(XD_STEP_EXECUTION_LISTENER_BEAN)) {
			this.stepListeners.add((StepListener) this.beanFactory.getBean(XD_STEP_EXECUTION_LISTENER_BEAN));
		}
		if (this.beanFactory.containsBean(XD_CHUNK_LISTENER_BEAN)) {
			this.stepListeners.add((StepListener) this.beanFactory.getBean(XD_CHUNK_LISTENER_BEAN));
		}
		if (this.beanFactory.containsBean(XD_ITEM_LISTENER_BEAN)) {
			this.stepListeners.add((StepListener) this.beanFactory.getBean(XD_ITEM_LISTENER_BEAN));
		}
		if (this.beanFactory.containsBean(XD_SKIP_LISTENER_BEAN)) {
			this.stepListeners.add((StepListener) this.beanFactory.getBean(XD_SKIP_LISTENER_BEAN));
		}
	}

	@Override
	public void destroy() throws Exception {
		Assert.notNull(groupName, "JobName should not be null");
		jobLocator.deleteJobRegistry(groupName);
		super.destroy();
	}
}
