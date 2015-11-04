/*
 *
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
 *  
 */

package org.springframework.xd.dirt.job.dsl;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Glenn Renfro
 */
public class ComposedJobUtil {

	public static final String MODULE_SUFFIX = "_COMPOSED";

	private static String orchestrationPatternString = "(\\|\\|" +//search for || in the definition
						"(?=([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))" + //make sure its not in quotes
						"|(\\&" + //or find a & in the definition
						"(?=([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))";// make sure its not in quotes

	private static String parameterPatternString = "(--" +//search for -- in the definition
			"(?=([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))"; //make sure its not in quotes

	private static Pattern orchestrationPattern = Pattern.compile(orchestrationPatternString);

	private static Pattern parameterPattern = Pattern.compile(parameterPatternString);

	/**
	 * Returns the composed module name for the job
	 * @param jobName - base name for the job
	 * @return the fully qualified name for composed job module
	 */
	public static String getComposedJobModuleName(String jobName){
		return jobName + MODULE_SUFFIX;
	}

	/**
	 * Is this definition a composed job definition
	 * @param definition the definition the user specified
	 * @return true if a valid composed job definition else false.
	 */
	public static boolean isComposedJobDefinition(String definition){
		boolean result = false;
		try {
			JobParser parser = new JobParser();
			JobSpecification jobSpecification = parser.parse(definition);
			if (jobSpecification.getJobReferences().size() > 1) {
				result = true;
			}
			//Check for single instanceof job with a transition
			if (jobSpecification.getJobReferences().size() == 1){
				JobReference reference = jobSpecification.getJobReferences().get(0);
				if( reference.transitions != null && reference.transitions.size() > 0){
					result = true;
				}
			}
		} catch (JobSpecificationException e) {
			//failed to parse, could still be a composed job.  Look for basic components
			// such as || or & if found return true.
			Matcher matcher = orchestrationPattern.matcher(definition);
			result = matcher.find();
		}
		return result;
	}

	/**
	 * Throws exception if user tries to create a single job instance with no transitions.
	 * @param definition the definition to be validated
	 * @param jobInstances collection of available job instances
	 */
	public static void validateNotSingleJobInstance(String definition, Collection<String> jobInstances) {
		JobSpecification jobSpecification = null;
		try {
			JobParser parser = new JobParser();
			jobSpecification = parser.parse(definition);
		} catch (JobSpecificationException e) {
			return; //failed to parse.
		}
		if (jobSpecification.getJobReferences().size() == 1) {
			String jobName = jobSpecification.getJobReferences().get(0).getName();
			if (jobInstances.contains(jobName)) {
				throw new IllegalStateException(String.format(
						"Job Instance specified  %s must use transitions.  Or just " +
								"launch the job instance directly", jobName));
			}
		}
	}

	public static String getPropertyDefinition() {
		return "options.timeout.description=The timeout for the slave jobs within this orchestration.  -1 indicates no timeout. \n" +
				"options.timeout.default=-1\n" +
				"options.timeout.type=long";
	}
	
	public static String getDefinitionParameters(String definition) {
		Matcher matcher = parameterPattern.matcher(definition);
		String result = "";
		if (matcher.find()){
			result = " " + definition.substring(matcher.start());
		}
		return result;
	}
	
}
