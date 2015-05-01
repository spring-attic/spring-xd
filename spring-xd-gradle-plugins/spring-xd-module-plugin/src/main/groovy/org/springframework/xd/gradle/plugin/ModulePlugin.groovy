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
 * Gradle plugin to support building module projects. This wraps the 'spring-boot' gradle plugin to build and package a 
 * module uber-jar using the 'bootRepackage' task.
 *
 * @author David Turanski
 * @since 1.1
 */
class ModulePlugin implements Plugin<Project> {

	/**
	 * Recursively exclude provided dependencies from the exported (module lib) scope.
	 *
	 * @param project the root project
	 * @param dependency the dependency
	 * @return ignored
	 */
	def excludeTransitiveDependencies(Project project, dependency) {
		project.configurations.exported.exclude group: dependency.moduleGroup, module: dependency.moduleName

		dependency.children.each {
			excludeTransitiveDependencies(project, it)
		}
	}

	@Override
	void apply(Project project) {
		project.apply plugin: 'spring-boot'
		project.apply plugin: 'propdeps'
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
				exclude group: 'org.springframework.xd', module: 'spring-xd-spark-streaming'
			}

			testCompile("org.springframework.xd:spring-xd-test:${project.springXdVersion}") {
				exclude group: 'org.springframework.xd', module: 'spring-xd-hadoop'
				exclude group: 'org.springframework.data', module: 'spring-data-hadoop'
				exclude group: 'org.springframework.data', module: 'spring-data-hadoop-test'
				exclude group: 'org.springframework.xd', module: 'spring-xd-spark-streaming'
			}

			messageBus("org.springframework.xd:spring-xd-messagebus-local:${project.springXdVersion}")

		}

		project.springBoot {
			layout = 'MODULE'
			customConfiguration = "exported"
		}

		project.task('configureModule') << {
			project.configurations.provided.resolvedConfiguration.firstLevelModuleDependencies.each {
				excludeTransitiveDependencies(project, it)
			}
		}

		project.bootRepackage.dependsOn 'configureModule'

	}
}
