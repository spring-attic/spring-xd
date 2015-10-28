/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.job.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xml.transform.StringResult;

/**
 * The root AST node for any AST parsed from a job specification.
 *
 * Andy Clement
 */
public class JobSpecification extends AstNode {

	/**
	 * The DSL text that was parsed to create this JobSpec.
	 */
	private String jobDefinitionText;

	/**
	 * The top level JobNode within this JobSpec.
	 */
	private JobNode jobNode;

	/**
	 * A list of jobs that were defined inline in the DSL text that was parsed
	 * to create this Ast. Computed on first reference.
	 */
	private List<JobDefinition> jobDefinitions;

	/**
	 * Any arguments specified at the end of the DSL, e.g. --timeout
	 */
	private ArgumentNode[] globalOptions;

	public JobSpecification(String jobDefinitionText, JobNode jobNode, ArgumentNode[] globalOptions) {
		super(jobNode == null ? 0 : jobNode.getStartPos(), jobNode == null ? 0 : jobNode.getEndPos());
		this.jobDefinitionText = jobDefinitionText;
		this.jobNode = jobNode;
		this.globalOptions = globalOptions;
	}

	public Map<String, String> getGlobalOptionsMap() {
		if (globalOptions == null) {
			return Collections.<String, String> emptyMap();
		}
		Map<String, String> optionsMap = new LinkedHashMap<String, String>();
		for (ArgumentNode option : globalOptions) {
			optionsMap.put(option.getName(), option.getValue());
		}
		return optionsMap;
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		s.append(jobNode.stringify(includePositionInfo));
		if (globalOptions != null) {
			for (ArgumentNode option : globalOptions) {
				s.append(" ");
				s.append(option.stringify(includePositionInfo));
			}
		}
		return s.toString();
	}

	public String getJobDefinitionText() {
		return jobDefinitionText;
	}

	public JobNode getJobNode() {
		return this.jobNode;
	}

	/**
	 * A shortcut (avoiding traversing the tree) that returns the list
	 * of all job definitions inlined somewhere in this AST. Computed
	 * on demand.
	 *
	 * @return a list of inlined job definitions defined in this AST
	 */
	public List<JobDefinition> getJobDefinitions() {
		if (jobDefinitions != null) {
			return jobDefinitions;
		}
		JobDefinitionLocator jdl = new JobDefinitionLocator();
		jdl.accept(this);
		jobDefinitions = jdl.getJobDefinitions();
		return jobDefinitions;
	}

	/**
	 * A shortcut (avoiding traversing the tree) that returns the list
	 * of all job references somewhere in this AST (references in
	 * transitions do not count).
	 *
	 * @return a list of job references in this AST
	 */
	public List<JobReference> getJobReferences() {
		JobReferenceLocator jrl = new JobReferenceLocator();
		jrl.accept(this);
		return jrl.getJobReferences();
	}

	/**
	 * Performs validation of the AST. Where the initial parse is about
	 * checking the syntactic structure, validation is about checking more
	 * semantic elements:<ul>
	 * <li>Do the inline job definitions refer to valid job modules?
	 * <li>Do the inline job definitions supply correct arguments?
	 * <li>Do the job references point to valid job definitions?
	 * </ul>
	 * @param jobDefinitionRepository a repository to check job definitions against
	 * @throws JobSpecificationException if validation fails
	 */
	public void validate(JobDefinitionRepository jobDefinitionRepository) {
		// TODO validate the job references (this will be done at deploy time but we could do it earlier)

	}

	/**
	 * @return this AST converted to a Graph form for display by Flo
	 */
	public Graph toGraph() {
		GraphGeneratorVisitor ggv = new GraphGeneratorVisitor();
		ggv.accept(this);
		return ggv.getGraph();
	}

	/**
	 * @param batchJobId the id that will be inserted into the XML document for the batch:job element
	 * @return this AST converted to an XML form
	 */
	public String toXML(String batchJobId) {
		return toXML(batchJobId, false);
	}

	/**
	 * @param batchJobId the id that will be inserted into the XML document for the batch:job element
	 * @param prettyPrint determine if the XML should be human readable.
	 * @return this AST converted to an XML form
	 */
	public String toXML(String batchJobId, boolean prettyPrint) {
		XMLGeneratorVisitor xgv = new XMLGeneratorVisitor(batchJobId, prettyPrint);
		xgv.accept(this);
		return xgv.getXmlString();
	}

	/**
	 * Basic visitor that simply collects up any inlined job definitions.
	 */
	static class JobDefinitionLocator extends JobSpecificationVisitor<Object> {

		List<JobDefinition> jobDefinitions = new ArrayList<JobDefinition>();

		public List<JobDefinition> getJobDefinitions() {
			return jobDefinitions;
		}

		@Override
		public Object walk(Object context, Flow sjs) {
			for (JobNode jobNode : sjs.getSeries()) {
				walk(context, jobNode);
			}
			return context;
		}

		@Override
		public Object walk(Object context, JobDefinition jd) {
			jobDefinitions.add(jd);
			return context;
		}

		@Override
		public Object walk(Object context, JobReference jr) {
			return context;
		}

		@Override
		public Object walk(Object context, Split pjs) {
			for (JobNode jobNode : pjs.getSeries()) {
				walk(context, jobNode);
			}
			return context;
		}

	}

	/**
	 * Basic visitor that simply collects up any job references (*not* those named in transitions)
	 */
	static class JobReferenceLocator extends JobSpecificationVisitor<Object> {

		List<JobReference> jobReferences = new ArrayList<JobReference>();

		public List<JobReference> getJobReferences() {
			return jobReferences;
		}

		@Override
		public Object walk(Object context, Flow sjs) {
			for (JobNode jobNode : sjs.getSeries()) {
				walk(context, jobNode);
			}
			return context;
		}

		@Override
		public Object walk(Object context, JobDefinition jd) {
			return context;
		}

		@Override
		public Object walk(Object context, JobReference jr) {
			jobReferences.add(jr);
			return context;
		}

		@Override
		public Object walk(Object context, Split pjs) {
			for (JobNode jobNode : pjs.getSeries()) {
				walk(context, jobNode);
			}
			return context;
		}

	}

	/**
	 * Visitor that produces an XML representation of the Job specification.
	 */
	static class XMLGeneratorVisitor extends JobSpecificationVisitor<Element[]> {

		/**
		 * containing document that can be used for element creation
		 */
		private Document doc;

		/**
		 * Where to append elements created during the visit
		 */
		private Element batchJobElement;

		/**
		 * Should the XML output be readable or compressed onto one line.
		 */
		private boolean prettyPrint;

		/**
		 * A stack that tracks the element that should have new children attached to it.
		 * Initially populated with batchJobElement from above.
		 */
		private Stack<Element> currentElement = new Stack<>();

		/**
		 * Counter for the numeric suffix to attach to generated split id attributes.
		 */
		private int splitIdCounter = 1;

		private List<String> jobRunnerBeanNames = new ArrayList<>();

		// As a new flow is entered, a new map is pushed here (popped on flow exit).
		// The map holds onto a map of transition names to allocated XML IDs within that flow.
		// This ensures all references to the same transition in a flow point to the same XML ID
		// But in a different flow the same transition names will point to a different XML ID.
		private Stack<Map<String, String>> transitionNamesToElementIdsInFlow = new Stack<>();

		// Knowing all the explicit job references in the tree means when seeing a transition
		// it can be determined if it is to a node not yet visited or something that will
		// never be visited (and so the step must be created right now).
		private Map<JobReference, String> jobReferencesToElementIds = new LinkedHashMap<>();

		private String xmlString;

		private String batchJobId;

		XMLGeneratorVisitor(String batchJobId, boolean prettyPrint) {
			this.batchJobId = batchJobId;
			this.prettyPrint = prettyPrint;
		}

		@Override
		protected void accept(JobSpecification jobSpec) {
			List<JobReference> jobReferences = jobSpec.getJobReferences();
			for (JobReference jr : jobReferences) {
				// Allocate unique XML Element IDs now, makes life easier later
				jobReferencesToElementIds.put(jr, getNextStepId(jr.getName()));
			}
			super.accept(jobSpec);
		}

		public String getXmlString() {
			return xmlString;
		}

		@Override
		public Element[] preJobSpecWalk() {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				DOMImplementation domImplementation = db.getDOMImplementation();

				// Generate:
				// <beans xmlns="http://www.springframework.org/schema/beans"
				//		  xmlns:batch="http://www.springframework.org/schema/batch"
				//		  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				//		  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
				//		    http://www.springframework.org/schema/batch http://www.springframework.org/schema/batch/spring-batch.xsd">
				this.doc = domImplementation.createDocument("http://www.springframework.org/schema/beans", "beans",
						null);
				doc.createElementNS("http://www.springframework.org/schema/batch", "batch");
				doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:batch",
						"http://www.springframework.org/schema/batch");
				doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi",
						"http://www.w3.org/2001/XMLSchema-instance");
				doc.getDocumentElement().setAttributeNS(
						"http://www.w3.org/2001/XMLSchema-instance",
						"xsi:schemaLocation",
						"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd "
								+
								"http://www.springframework.org/schema/batch http://www.springframework.org/schema/batch/spring-batch.xsd");
				// Setting this 'again' to get it on the front and look more like the above.
				doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns",
						"http://www.springframework.org/schema/beans");

				// Generate: <bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor"/>
				Element taskExecutor = doc.createElement("bean");
				doc.getDocumentElement().appendChild(taskExecutor);
				taskExecutor.setAttribute("id", "taskExecutor");
				taskExecutor.setAttribute("class", "org.springframework.core.task.SimpleAsyncTaskExecutor");

				// Generate: <batch:job id="streamName" xmlns="http://www.springframework.org/schema/batch">
				this.batchJobElement = doc.createElement("batch:job");
				doc.getDocumentElement().appendChild(batchJobElement);
				batchJobElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns",
						"http://www.springframework.org/schema/batch");
				if (batchJobId != null) {
					batchJobElement.setAttribute("id", batchJobId);
				}
				transitionNamesToElementIdsInFlow.push(new LinkedHashMap<String, String>());
				this.currentElement.push(batchJobElement);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected problem building XML representation", e);
			}
			return null;
		};

		/**
		 * Determine if a step element has any &lt;next.../&gt; elements. If it does then
		 * it is mapping the exit space from this step.
		 * @param step the XML element representing the step
		 * @return true if the supplied step specifies any transitions via next elements
		 */
		private boolean isMappingExitSpace(Element step) {
			NodeList children = step.getChildNodes();
			for (int c = 0; c < children.getLength(); c++) {
				String nodeName = children.item(c).getNodeName();
				if (nodeName.equals("next") || nodeName.equals("end") || nodeName.equals("fail")) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check if the XML element for a step indicates a &lt;next.../&gt; element for a specific
		 * exit status.
		 * @param step the XML element representing the step
		 * @param exitStatus the exit status to check for
		 * @return true if the step specifies a next attribute for the supplied exit status
		 */
		private boolean isMappingExitStatus(Element step, String exitStatus) {
			NodeList children = step.getChildNodes();
			for (int c = 0; c < children.getLength(); c++) {
				String nodeName = children.item(c).getNodeName();
				if (nodeName.equals("next") || nodeName.equals("end") || nodeName.equals("fail")) {
					String onAttributeValue = children.item(c).getAttributes().getNamedItem("on").getNodeValue();
					if (onAttributeValue.equals(exitStatus)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Create a &lt;next on=  to=&gt; attribute and attach it to the supplied step element.
		 * @param step the step that will get the new attribute
		 * @param jobExitStatus the jobExitStatus to fill in as the 'on' attribute value field
		 * @param targetId the target XML ID to fill in as the 'to' attribute value field
		 */
		private void addNextAttribute(Element step, String jobExitStatus, String targetId) {
			Element next = doc.createElement("next");
			next.setAttribute("on", jobExitStatus);
			next.setAttribute("to", targetId);
			step.appendChild(next);
		}

		/**
		 * Create a &lt;fail on=&gt; attribute and attach it to the supplied step element.
		 * @param step the step that will get the new attribute
		 * @param jobExitStatus the jobExitStatus to fill in as the 'on' attribute value field
		 */
		private void addFailAttribute(Element step, String jobExitStatus) {
			Element fail = doc.createElement("fail");
			fail.setAttribute("on", jobExitStatus);
			step.appendChild(fail);
		}

		/**
		 * Create a &lt;end on=&gt; attribute and attach it to the supplied step element.
		 * @param step the step that will get the new attribute
		 * @param jobExitStatus the jobExitStatus to fill in as the 'on' attribute value field
		 */
		private void addEndAttribute(Element step, String jobExitStatus) {
			Element fail = doc.createElement("end");
			fail.setAttribute("on", jobExitStatus);
			step.appendChild(fail);
		}

		@Override
		public void postJobSpecWalk(Element[] elements, JobSpecification jobSpec) {
			if (elements != null) {
				// These are the final elements that were visited
				for (Element element : elements) {
					if (isMappingExitSpace(element)) {
						if (!isMappingExitStatus(element, "*")) {
							addFailAttribute(element, "*");
						}
					}
				}
			}
			Map<String, String> transitionStepsToCreate = transitionNamesToElementIdsInFlow.pop();
			for (Map.Entry<String, String> transitionStepToCreate : transitionStepsToCreate.entrySet()) {
				Element step = createStep(transitionStepToCreate.getValue(), transitionStepToCreate.getKey());
				currentElement.peek().appendChild(step);
			}

			Set<String> generatedBeans = new HashSet<>();
			for (String jobRunnerBeanName : jobRunnerBeanNames) {
				if (generatedBeans.contains(jobRunnerBeanName)) {
					continue;
				}
				// Producing:
				// <bean class="org.springframework.xd.dirt.batch.tasklet.JobLaunchingTasklet" id="jobRunner-bbb" scope="step">
				//        <constructor-arg ref="messageBus"/>
				//        <constructor-arg ref="jobDefinitionRepository"/>
				//        <constructor-arg ref="xdJobRepository"/>
				//        <constructor-arg value="bbb"/>
				//        <constructor-arg value="${timeout}"/>
				// </bean>
				Element bean = doc.createElement("bean");
				bean.setAttribute("scope", "step");
				bean.setAttribute("class", "org.springframework.xd.dirt.batch.tasklet.JobLaunchingTasklet");
				bean.setAttribute("id", "jobRunner-" + jobRunnerBeanName);
				addConstructorArg(bean, "ref", "messageBus");
				addConstructorArg(bean, "ref", "jobDefinitionRepository");
				addConstructorArg(bean, "ref", "xdJobRepository");
				addConstructorArg(bean, "value", jobRunnerBeanName);
				addConstructorArg(bean, "value", "${timeout}");
				this.doc.getElementsByTagName("beans").item(0).appendChild(bean);
				generatedBeans.add(jobRunnerBeanName);
			}
			try {
				// Write the content
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				javax.xml.transform.Transformer transformer;
				transformer = transformerFactory.newTransformer();
				if (prettyPrint) {
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				}
				DOMSource source = new DOMSource(doc);
				StringResult sr = new StringResult();
				transformer.transform(source, sr);
				xmlString = sr.toString().trim();
			}
			catch (TransformerException e) {
				throw new IllegalStateException("Unexpected problem building XML representation", e);
			}
		}

		private void addConstructorArg(Element bean, String attributeName, String argName) {
			Element ctorArgElement = doc.createElement("constructor-arg");
			ctorArgElement.setAttribute(attributeName, argName);
			bean.appendChild(ctorArgElement);
		}

		@Override
		public Element[] walk(Element[] context, Flow jn) {
			boolean inSplit = currentElement.peek().getTagName().equals("split");
			// Only need the flow element if nested in a split
			if (inSplit) {
				Element flow = doc.createElement("flow");
				currentElement.peek().appendChild(flow);
				currentElement.push(flow);
			}
			Element[] result = context;
			transitionNamesToElementIdsInFlow.push(new LinkedHashMap<String, String>());
			for (JobNode j : jn.getSeries()) {
				result = walk(result, j);
			}
			Map<String, String> transitionStepsToCreate = transitionNamesToElementIdsInFlow.pop();
			for (Map.Entry<String, String> transitionStepToCreate : transitionStepsToCreate.entrySet()) {
				Element step = createStep(transitionStepToCreate.getValue(), transitionStepToCreate.getKey());
				currentElement.peek().appendChild(step);
			}
			if (inSplit) {
				currentElement.pop();
			}
			return result;
		}

		@Override
		public Element[] walk(Element[] context, JobDefinition jd) {
			// TODO this code needs some rework to match XML gen for JobReference but we don't
			// support JobDefinitions in the first version of the DSL.
			Element step = doc.createElement("step");
			step.setAttribute("id", jd.getJobName());
			Element tasklet = doc.createElement("tasklet");
			String jobRunnerId = "jobRunner-" + jd.getJobName();
			tasklet.setAttribute("ref", jobRunnerId);
			jobRunnerBeanNames.add(jd.getName());
			step.appendChild(tasklet);
			Element next = null;
			if (jd.hasTransitions()) {
				for (Transition t : jd.transitions) {
					if (t.getTargetJobName().equals(Transition.FAIL)) {
						addFailAttribute(step, t.getStateName());
					}
					else if (t.getTargetJobName().equals(Transition.END)) {
						addEndAttribute(step, t.getStateName());
					}
					else {
						addNextAttribute(step, t.getStateName(), t.getTargetJobName());
					}
				}
				// If there are transitions, it is necessary to ensure the whole exit space is covered from this
			}
			if (context != null) {
				// context is an array of earlier elements that should point to this one
				Element[] elements = context;
				for (Element element : elements) {
					next = doc.createElement("next");
					next.setAttribute("on", "COMPLETED");
					next.setAttribute("to", jd.getJobName());
					element.appendChild(next);
				}
			}
			this.currentElement.peek().appendChild(step);
			return new Element[] { step };
		}

		private List<String> allocatedStepIds = new ArrayList<>();

		/**
		 * Determine the next unique ID we can use for an XML element.
		 */
		private String getNextStepId(String prefix) {
			if (!allocatedStepIds.contains(prefix)) {
				// Avoid number suffix for the first one
				allocatedStepIds.add(prefix);
				return prefix;
			}
			int suffix = 1;
			String proposal = null;
			do {
				proposal = new StringBuilder(prefix).append(Integer.toString(suffix++)).toString();
			}
			while (allocatedStepIds.contains(proposal));
			allocatedStepIds.add(proposal);
			return proposal;
		}


		/**
		 * Visit a job reference. Rules:
		 * <ul>
		 * <li>The flow surrounding element for the step is created if inside a split
		 * </ul>
		 */
		@Override
		public Element[] walk(Element[] context, JobReference jr) {
			// Producing this kind of construct:
			// <flow">
			//   <step id="sqoop-6e44">
			//	   <tasklet ref="jobRunner-6e44"/>
			//	   <next on="COMPLETED" to="sqoop-e07a"/>
			//	   <next on="FAILED" to="kill1"/>
			//     <fail on="*"/>
			//   </step>
			// </flow>

			// When a split branch only contains a single job reference, no surrounding Flow object is created,
			// so the flow block needs creating here in this case.
			boolean inSplit = currentElement.peek().getTagName().equals("split");
			if (inSplit) {
				Element flow = doc.createElement("flow");
				currentElement.peek().appendChild(flow);
				currentElement.push(flow);
			}
			String stepId = jobReferencesToElementIds.get(jr);
			Element step = createStep(stepId, jr.getName());
			currentElement.peek().appendChild(step);
			jobRunnerBeanNames.add(jr.getName());
			boolean explicitWildcardExit = false;
			if (jr.hasTransitions()) {
				for (Transition t : jr.transitions) {
					if (t.getStateName().equals("*")) {
						explicitWildcardExit = true;
					}
					String targetJob = t.getTargetJobName();
					if (targetJob.equals(Transition.END)) {
						addEndAttribute(step, t.getStateName());
						continue;
					}
					else if (targetJob.equals(Transition.FAIL)) {
						addFailAttribute(step, t.getStateName());
						continue;
					}
					Map<String, String> transitionNamesToElementIdsInCurrentFlow = transitionNamesToElementIdsInFlow.peek();
					if (transitionNamesToElementIdsInCurrentFlow.containsKey(targetJob)) {
						// already exists, share the ID
						targetJob = transitionNamesToElementIdsInCurrentFlow.get(targetJob);
					}
					else {
						// Is this a reference to a job that already exists elsewhere in this composed job definition?
						String id = getReferenceToExistingJob(targetJob);
						if (id == null) {
							// create an entry, this is the first reference to this target job in this flow
							id = getNextStepId(targetJob);
							transitionNamesToElementIdsInCurrentFlow.put(targetJob, id);
							if (inSplit) {
								// If a job reference is directly inside a split with no surrounding flow to create
								// the steps collected in 'existingTransitionSteps' then it needs to be done here.
								Element transitionStep = createStep(id, t.getTargetJobName());
								currentElement.peek().appendChild(transitionStep);
							}
						}
						targetJob = id;
					}
					addNextAttribute(step, t.getStateName(), targetJob);
					jobRunnerBeanNames.add(t.getTargetJobName());
				}
				if (inSplit) {
					// The split is the element that will be analyzed to see if all exit
					// statuses are covered. So for a job reference created here we need to
					// ensure we do the analysis here.
					if (!isMappingExitStatus(step, "*")) {
						addFailAttribute(step, "*");
					}
				}
			}
			if (context != null) {
				// context is an array of earlier elements that should be updated now to point to this one
				for (Element element : context) {
					addNextAttribute(element, "COMPLETED", stepId);
					if (!isMappingExitStatus(element, "*")) {
						addFailAttribute(element, "*");
					}
				}
			}
			if (inSplit) {
				currentElement.pop();
			}
			return explicitWildcardExit ? new Element[] {} : new Element[] { step };
		}

		@Override
		public Element[] walk(Element[] context, Split pjs) {
			// Producing this kind of output:
			// <split id="split1" task-executor="taskExecutor">
			//   ...
			// </split>
			Element split = doc.createElement("split");
			String splitId = "split" + (splitIdCounter++);
			split.setAttribute("task-executor", "taskExecutor");
			split.setAttribute("id", splitId);

			if (context != null) {
				// context is an array of earlier elements that should point to this one
				for (Element element : context) {
					addNextAttribute(element, "COMPLETED", splitId);
					if (!isMappingExitStatus(element, "*")) {
						addFailAttribute(element, "*");
					}
				}
			}

			currentElement.peek().appendChild(split);
			currentElement.push(split);
			Element[] inputContext = new Element[] {};//context;
			Element[] result = new Element[0];
			for (JobNode jn : pjs.getSeries()) {
				transitionNamesToElementIdsInFlow.push(new LinkedHashMap<String, String>());
				Object outputContext = walk(inputContext, jn);
				transitionNamesToElementIdsInFlow.pop();
				result = merge(result, outputContext);
			}
			currentElement.pop();
			// The only element from here to connect to the 'next thing' is the split node.
			// This means only the split gets a 'next on="*"' element.
			return new Element[] { split };
		}

		private Element[] merge(Element[] input, Object additional) {
			Element[] additionalArrayData = (Element[]) additional;
			Element[] result = new Element[input.length + additionalArrayData.length];
			System.arraycopy(input, 0, result, 0, input.length);
			System.arraycopy(additionalArrayData, 0, result, input.length, additionalArrayData.length);
			return result;
		}

		private Element createStep(String stepId, String jobRunnerBeanIdSuffix) {
			Element step = doc.createElement("step");
			step.setAttribute("id", stepId);
			Element tasklet = doc.createElement("tasklet");
			tasklet.setAttribute("ref", "jobRunner-" + jobRunnerBeanIdSuffix);
			step.appendChild(tasklet);
			return step;
		}

		private String getReferenceToExistingJob(String jobName) {
			for (Map.Entry<JobReference, String> jrEntry : jobReferencesToElementIds.entrySet()) {
				if (jrEntry.getKey().getName().equals(jobName)) {
					return jrEntry.getValue();
				}
			}
			return null;
		}

	}

	/**
	 * Visitor that produces a Graph representation of the Job specification suitable
	 * for display by Flo.
	 */
	static class GraphGeneratorVisitor extends JobSpecificationVisitor<int[]> {

		private int id = 0;

		private Map<String, Node> createdNodes = new HashMap<>();

		// As the visit proceeds different contexts are entered/left - into a flow, into a split, etc.
		// The context object on top of the stack allows a particular visit method
		// to know what the current context is.
		private Stack<Context> contexts = new Stack<>();

		/**
		 * Encapsulates the current context
		 */
		static class Context {

			// Set when processing the last element of a flow
			boolean isEndOfFlow = false;

			List<Integer> flowExits;

			Map<String, Node> nodesSharedInFlow = new LinkedHashMap<String, Node>();

			// Set whilst in a flow, allows us to recognize forward references from
			// transitions (references to jobs named explicitly later in the flow)
			List<String> jobsInFlow;

			Context(boolean inFlow) {
				if (inFlow) {
					flowExits = new ArrayList<Integer>();
				}
			}

			public void setJobsInFlow(List<String> jobsNamedInFlow) {
				jobsInFlow = jobsNamedInFlow;
			}
		}

		// The constructed graph elements (nodes, links, properties):

		private List<Node> nodes = new ArrayList<>();

		private List<Link> links = new ArrayList<>();

		private Map<String, String> properties = new LinkedHashMap<String, String>();

		public Graph getGraph() {
			Graph g = new Graph(nodes, links, properties);
			return g;
		}

		@Override
		public int[] preJobSpecWalk() {
			// Insert a START node at the beginning of the graph
			Node node = new Node(Integer.toString(id++), "START");
			nodes.add(node);
			contexts.push(new Context(false));
			return new int[] { 0 };
		}

		@Override
		public void postJobSpecWalk(int[] finalNodes, JobSpecification jobSpec) {
			// Insert an END node
			int endId = id++;
			Node endNode = new Node(Integer.toString(endId), "END");
			nodes.add(endNode);
			contexts.pop();

			// Handle special case where $END has been used. For example:
			// foo | oranges=$END
			// <foo | oranges = $END & xx>
			// In both these cases it looks odd if the graph shows a line between two $ENDs, so
			// they are collapsed together and the links from the former $END are forwarded to the
			// final $END. (The former $END node is then deleted).
			for (int i : finalNodes) {
				// Is the incoming node a $END node?
				Node incomingNode = findNodeById(i);
				if (incomingNode.name.equals("END")) {
					List<Link> linksToUpdate = getLinksTo(i);
					for (Link link : linksToUpdate) {
						link.updateTo(Integer.toString(endId));
					}
					// Everything to this END node has been re-routed, delete it now
					nodes.remove(incomingNode);
				}
				else {
					// Just link the final node to the real $END
					links.add(new Link(i, endId));
				}
			}
			Map<String, String> options = jobSpec.getGlobalOptionsMap();
			if (options.size() != 0) {
				for (Map.Entry<String, String> option : options.entrySet()) {
					properties.put(option.getKey(), option.getValue());
				}
			}
		}

		@Override
		public int[] walk(int[] context, Flow jn) {
			int[] result = context;
			contexts.push(new Context(true));

			// Compute the jobs in this flow to cope with
			// forward references
			List<String> jobsNamedInFlow = new ArrayList<String>();
			for (JobNode j : jn.getSeries()) {
				if (j instanceof JobDescriptor) {
					jobsNamedInFlow.add(((JobDescriptor) j).getName());
				}
			}
			contexts.peek().setJobsInFlow(jobsNamedInFlow);

			Iterator<JobNode> seriesIterator = jn.getSeries().iterator();
			while (seriesIterator.hasNext()) {
				JobNode j = seriesIterator.next();
				if (!seriesIterator.hasNext()) {
					// For the last element in the series, set this flag
					contexts.peek().isEndOfFlow = true;
				}
				result = walk(result, j);
			}

			// Merge the outputs from the last node in the flow
			// with any transitional exits from the flow
			List<Integer> exits = contexts.pop().flowExits;
			for (int r : result) {
				exits.add(r);
			}
			result = toIntArray(exits);
			return result;
		}

		@Override
		public int[] walk(int[] context, Split pjs) {
			int[] inputContext = context;

			if (inputContext.length > 1) {
				// Cannot directly connect a split to a split, we need a SYNC node
				// to simulate fan-in/fan-out (inputContext.length > 1 indicates a
				// previous split).
				int nextId = id++;
				Node node = new Node(Integer.toString(nextId), "SYNC");
				nodes.add(node);
				for (int i : inputContext) {
					Link l = new Link(i, nextId);
					links.add(l);
				}
				// Now create new context that contains only the sync node output
				inputContext = new int[] { nextId };
			}

			int[] result = new int[0];
			for (JobNode jn : pjs.getSeries()) {
				contexts.push(new Context(false));
				Object outputContext = walk(inputContext, jn);
				contexts.pop();
				result = merge(result, outputContext);
			}
			return result;
		}

		@Override
		public int[] walk(int[] context, JobReference jr) {
			Map<String, String> properties = null;
			ArgumentNode[] args = jr.getArguments();
			if (args != null && args.length != 0) {
				properties = new LinkedHashMap<>();
				for (ArgumentNode arg : args) {
					properties.put(arg.getName(), arg.getValue());
				}
			}
			Node node;
			int nextId;

			// Check if this walk has hit something created earlier in this flow
			// which may have occurred if a transition referred to it, e.g.
			// foo | fail=bar || goo || bar
			// where 'walk'ing foo will have created a node for bar - reuse it.

			Node existingNode = contexts.peek().nodesSharedInFlow.get(jr.getName());
			if (existingNode != null) {
				// Found it, reuse it here
				node = existingNode;
				nextId = Integer.parseInt(existingNode.id);
			}
			else {
				// Not already in this flow, create a new one
				nextId = id++;
				node = new Node(Integer.toString(nextId), jr.getName(), null, properties);
				nodes.add(node);
			}
			createdNodes.put(jr.getName(), node);

			// Should nodes created in the mainline visit be inserted into the shared set?
			// (In addition to those created for transitions). Do we need it to support
			// back references (loops?)

			// Create links from the previous nodes to this one
			if (context != null) {
				int[] s = context;
				for (int i : s) {
					Link l = new Link(i, nextId);
					links.add(l);
				}
			}

			// Process all the transitions
			boolean explicitWildcardUsed = false; // Is '*' used in any transition?
			boolean isMappingExitSpace = false; // Are they mapping the exit space (i.e. have transitions)

			// This list will collect transitional exits and the 'usual' COMPLETED exit if it
			// applies (i.e. they haven't used transitions that prevent it applying)
			List<Integer> allExitsToReturn = new ArrayList<>();
			if (jr.hasTransitions()) {
				isMappingExitSpace = true;
				List<Integer> flowExits = contexts.peek().flowExits;
				for (Transition t : jr.getTransitions()) {
					if (t.getStateNameInDSLForm().equals("'*'")) {
						explicitWildcardUsed = true;
					}
					String jobExitStatus = t.getStateNameInDSLForm();
					String targetJobName = t.getTargetJobName();
					int transitionTargetId = buildTransition(nextId, jobExitStatus, targetJobName);
					// If something was created and it isn't a node further down the flow, record the exit
					if (transitionTargetId != -1 && !isForwardReferenceInFlow(targetJobName)) {
						if (flowExits != null) {
							flowExits.add(transitionTargetId);
						}
						else {
							allExitsToReturn.add(transitionTargetId);
						}
					}
				}
			}

			// Determine if this node should be linked to following nodes.
			if (isMappingExitSpace) {
				// Only automatic if not the last one in the flow
				// The latter condition here is checking if this is a jobreference visit
				// immediately inside a split (with no surrounding flow) which should
				// be treated the same as the end of a flow
				if (!explicitWildcardUsed) {
					if ((!contexts.peek().isEndOfFlow && contexts.peek().flowExits != null)) {
						allExitsToReturn.add(nextId);
					}
				}
			}
			else {
				allExitsToReturn.add(nextId);
			}
			return toIntArray(allExitsToReturn);
		}

		int[] merge(int[] input, Object additional) {
			int[] additionalArrayData = (int[]) additional;
			int[] result = new int[input.length + additionalArrayData.length];
			System.arraycopy(input, 0, result, 0, input.length);
			System.arraycopy(additionalArrayData, 0, result, input.length, additionalArrayData.length);
			return result;
		}

		public Node findNodeInList(List<Integer> nodesInScope, String name) {
			if (nodesInScope != null) {
				for (Integer i : nodesInScope) {
					for (Node n : nodes) {
						if (n.id.equals(i)) {
							// This one is in scope
							if (n.name.equals(name)) {
								return n;
							}
						}
					}
				}
			}
			return null;
		}

		/**
		 * Check if the specified job name refers to a job mentioned later
		 * in this flow.
		 * @param targetJobName the job name to search for
		 * @return true if it is a reference to a job later in the flow
		 */
		private boolean isForwardReferenceInFlow(String searchJobName) {
			List<String> jobsInFlow = contexts.peek().jobsInFlow;
			if (jobsInFlow != null) {
				for (String jobname : jobsInFlow) {
					if (jobname.equals(searchJobName)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Construct a transition link, and possibly a new node if a suitable candidate does not already exist.
		 * If a transition has already referenced a particular job in the same flow, reuse it. If not then
		 * create a new node and link to it.
		 *
		 * @param fromJobNodeId the id of the jobnode from which the transition is occurring
		 * @param jobExitStatus the job exit status attached to this transition
		 * @param targetJobName the transition target
		 * @return the ID of the transition target node if a new node created, otherwise -1
		 */
		private int buildTransition(int fromJobNodeId, String jobExitStatus, String targetJobName) {
			boolean createdNewTarget = false;
			if (targetJobName.equals(Transition.FAIL)) {
				targetJobName = "FAIL";
			}
			else if (targetJobName.equals(Transition.END)) {
				targetJobName = "END";
			}
			Node targetNode = contexts.peek().nodesSharedInFlow.get(targetJobName);//scope.peek().get(targetJobName);
			int transitionTargetId;
			if (targetNode == null) {
				transitionTargetId = id++;
				targetNode = new Node(Integer.toString(transitionTargetId), targetJobName);
				nodes.add(targetNode);
				createdNewTarget = true;
			}
			else {
				transitionTargetId = Integer.parseInt(targetNode.id);
			}
			links.add(new Link(fromJobNodeId, transitionTargetId, jobExitStatus));
			//scope.peek().put(targetJobName, targetNode);
			contexts.peek().nodesSharedInFlow.put(targetJobName, targetNode);
			return createdNewTarget ? transitionTargetId : -1;
		}

		@Override
		public int[] walk(int[] context, JobDefinition jd) {
			// Inline job definitions not yet supported
			//			int nextId = id++;
			//			Map<String, String> properties = null;
			//			ArgumentNode[] args = jd.getArguments();
			//			if (args != null && args.length != 0) {
			//				properties = new LinkedHashMap<>();
			//				for (ArgumentNode arg : args) {
			//					properties.put(arg.getName(), arg.getValue());
			//				}
			//			}
			//			Map<String, String> metadata = new HashMap<>();
			//			metadata.put(Node.METADATAKEY_JOBMODULENAME, jd.getJobModuleName());
			//			Node node = new Node(Integer.toString(nextId), jd.getJobName(), metadata, properties);
			//			nodes.add(node);
			//			createdNodes.put(node.name, node);
			//			if (context != null) {
			//				int[] s = context;
			//				for (int i : s) {
			//					Link l = new Link(i, nextId);
			//					links.add(l);
			//				}
			//			}
			//			if (jd.hasTransitions()) {
			//				for (Transition t : jd.getTransitions()) {
			//					transitions.add(new TransitionToMap(nextId, t.getStateNameInDSLForm(), t.getTargetJobName()));
			//				}
			//			}
			//			return new int[] { nextId };
			return new int[] {};
		}

		private List<Link> getLinksTo(int i) {
			List<Link> result = new ArrayList<>();
			for (Link link : links) {
				if (i == Integer.parseInt(link.to)) {
					result.add(link);
				}
			}
			return result;
		}

		private Node findNodeById(int i) {
			for (Node node : nodes) {
				if (Integer.parseInt(node.id) == i) {
					return node;
				}
			}
			return null;
		}

		private int[] toIntArray(List<Integer> integers) {
			int[] result = new int[integers.size()];
			for (int i = 0; i < integers.size(); i++) {
				result[i] = integers.get(i);
			}
			return result;
		}

	}

	public JobDefinition getJobDefinition(String jobName) {
		for (JobDefinition jd : getJobDefinitions()) {
			if (jd.getJobName().equals(jobName)) {
				return jd;
			}
		}
		return null;
	}

	/**
	 * Pretty print the text for this job specification, including appropriate
	 * newlines and indentation.
	 * @return formatted job specification.
	 */
	public String format() {
		return jobNode.format(0);
	}
}
