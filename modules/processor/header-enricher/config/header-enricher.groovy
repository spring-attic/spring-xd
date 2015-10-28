import groovy.json.JsonSlurper

/*
 * Copyright 2015 the original author or authors.
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
beans {
	xmlns([si: 'http://www.springframework.org/schema/integration'])

	def headers = environment.getProperty('headers')
	def overwrite = environment.getProperty('overwrite')
	def parser = new JsonSlurper()
	def result = parser.parseText(headers)

	si.channel(id:'input')
	si.channel(id:'output')

	si.'header-enricher'('input-channel':'input', 'output-channel':'output', 'default-overwrite':overwrite) {
		result.each {k,v->
			si.'header'(name:k, expression:v)
		}
	}
}
