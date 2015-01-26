/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * Gradle plugin to support module projects
 * @author David Turanski
 * @since 1.1
 */
class ModulePlugin implements Plugin<Project> {

	/**
	 * *
	 * @param project
	 * @param dependency
	 * @return
	 */
	def excludeTransitiveDependencies(Project project, dependency) {
		project.configurations.exported.exclude group: dependency.moduleGroup, module: dependency.moduleName

		dependency.children.each {
			excludeTransitiveDependencies(project, it)
		}
	}

	/**
	 * *
	 * @param project
	 */
	void apply(Project project) {
		project.apply plugin: 'spring-boot'
		project.apply plugin: 'propdeps'
		project.apply plugin: 'propdeps-maven'
		project.apply plugin: 'propdeps-idea'
		project.apply plugin: 'propdeps-eclipse'

		project.configurations {
			exported {
				extendsFrom project.configurations.runtime
			}
			messageBus {}
		}

		project.configurations.provided.transitive = true
		project.configurations.messageBus.transitive = false

		project.dependencies {
			provided("org.springframework.xd:spring-xd-dirt:${project.springXdVersion}") {
				exclude group: 'org.springframework.xd', module: 'spring-xd-hadoop'
			}

			testCompile("org.springframework.xd:spring-xd-test:${project.springXdVersion}") {
				exclude group: 'org.springframework.xd', module: 'spring-xd-hadoop'
			}

			messageBus("org.springframework.xd:spring-xd-messagebus-local:${project.springXdVersion}")

			provided("org.apache.hadoop:hadoop-common:2.6.0") {
				exclude group: 'javax.servlet'
			}
		}


		project.springBoot {
			layout = 'MODULE'
			customConfiguration = "exported"
		}

		project.test {
			systemProperty 'XD_HOME', project.rootDir
		}

		project.task('configureModule') << {
			project.configurations.provided.resolvedConfiguration.firstLevelModuleDependencies.each {
				excludeTransitiveDependencies(project, it)
			}
		}

		project.task('copyLocalMessageBus', type: Copy) {
			from project.configurations.messageBus
			into project.file('lib/messagebus/local')
		}

		project.processTestResources.dependsOn 'copyLocalMessageBus'

		project.bootRepackage.dependsOn 'configureModule'

	}
}