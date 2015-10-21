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
	
	public static String getComposedJobModuleName(String jobName){
		return jobName + MODULE_SUFFIX;
	}
	
	public static boolean isComposedJobDefinition(String definition){
		Matcher matcher = orchestrationPattern.matcher(definition);
		return matcher.find();
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
