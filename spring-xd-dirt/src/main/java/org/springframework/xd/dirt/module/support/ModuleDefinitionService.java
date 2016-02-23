/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.xd.dirt.module.support;

import static org.springframework.xd.dirt.stream.ParsingContext.module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.xd.dirt.job.dsl.ComposedJobUtil;
import org.springframework.xd.dirt.job.dsl.JobParser;
import org.springframework.xd.dirt.module.DependencyException;
import org.springframework.xd.dirt.module.ModuleAlreadyExistsException;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.module.UploadedModuleDefinition;
import org.springframework.xd.dirt.module.WritableModuleRegistry;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * A service that knows how to handle registration of new module definitions, be it through composition or
 * upload of actual 'bytecode'. Handles all bookkeeping (such as existence checking, dependency tracking, <i>etc.</i>)
 * that is common to all registration scenarios.
 *
 * <p>Also adds pagination to {@code find*()} methods of {@code ModuleRegistry} after the fact.</p>
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
public class ModuleDefinitionService {

	private final WritableModuleRegistry registry;

	private final XDStreamParser parser;

	private final ModuleDependencyRepository dependencyRepository;

	private final PagingUtility<ModuleDefinition> pagingUtility = new PagingUtility<ModuleDefinition>();

	private final JobParser composedJobParser;

	@Autowired
	public ModuleDefinitionService(WritableModuleRegistry registry, XDStreamParser parser, ModuleDependencyRepository dependencyRepository) {
		this.registry = registry;
		this.parser = parser;
		this.dependencyRepository = dependencyRepository;
		this.composedJobParser = new JobParser();
	}

	public ModuleDefinition findDefinition(String name, ModuleType type) {
		return registry.findDefinition(name, type);
	}

	public Page<ModuleDefinition> findDefinitions(Pageable pageable, String name) {
		List<ModuleDefinition> raw = registry.findDefinitions(name);
		return pagingUtility.getPagedData(pageable, raw);
	}

	public Page<ModuleDefinition> findDefinitions(Pageable pageable, ModuleType type) {
		List<ModuleDefinition> raw = registry.findDefinitions(type);
		return pagingUtility.getPagedData(pageable, raw);
	}

	public Page<ModuleDefinition> findDefinitions(Pageable pageable) {
		List<ModuleDefinition> raw = registry.findDefinitions();
		return pagingUtility.getPagedData(pageable, raw);
	}

	public ModuleDefinition compose(String name, ModuleType typeHint, String dslDefinition, boolean force) {
		ModuleDefinition moduleDefinition = null;
		if(typeHint == ModuleType.job){
			moduleDefinition = composeJob(name, typeHint, dslDefinition, force);
		}
		else{
			moduleDefinition = composeStream(name, typeHint, dslDefinition, force);
		}
		return moduleDefinition;
	}

	private ModuleDefinition composeJob(String name, ModuleType typeHint, String dslDefinition, boolean force){
		assertModuleUpdatability(name, typeHint, force);
		String composedJobXml = composedJobParser.parse(dslDefinition).toXML(name);
		byte bytes[] = createComposedJobJar(composedJobXml);
		ModuleDefinition moduleDefinition = new UploadedModuleDefinition(name, typeHint, bytes) ;
		Assert.isTrue(this.registry.registerNew(moduleDefinition), moduleDefinition + " could not be saved");
		return moduleDefinition;
	}

	private byte[] createComposedJobJar(String xml) {

		JarOutputStream target = null;
		try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
			target = new JarOutputStream(outStream);
			JarEntry entry = new JarEntry("config/");
			target.putNextEntry(entry);
			target.closeEntry();
			writeXML(target, xml);
			writeParameters(target, ComposedJobUtil.getPropertyDefinition());

			//This needs to be explicitly closed before we get the bytes so that the
			//compression is complete.  target.flush() does not work for this.
			target.close();
			return outStream.toByteArray();
		}
		catch (Exception e) {
			try{
				if(target != null){
					target.close();
				}
			}catch (IOException ie){
				throw new IllegalStateException(e.getMessage(), e);
			}
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private void writeXML(JarOutputStream target, String xml) {
		try (InputStream in = new ByteArrayInputStream(xml.getBytes(Charset.forName(StandardCharsets.UTF_8.name())))) {
			JarEntry entry = new JarEntry("config/composedjob.xml");
			target.putNextEntry(entry);
			StreamUtils.copy(in, target);
			target.closeEntry();
		}
		catch(IOException ioe){
			throw new IllegalStateException(ioe.getMessage(), ioe);
		}
	}
	private void writeParameters(JarOutputStream target, String parameters) {
		try (InputStream in = new ByteArrayInputStream(parameters.getBytes(Charset.forName("UTF-8")))) {
			JarEntry entry = new JarEntry("config/composedjob.properties");
			target.putNextEntry(entry);
			StreamUtils.copy(in, target);
			target.closeEntry();
		}
		catch(IOException ioe){
			throw new IllegalStateException(ioe.getMessage(), ioe);
		}
	}
	private ModuleDefinition composeStream(String name, ModuleType typeHint, String dslDefinition, boolean force){
		// TODO: pass typeHint to parser (XD-2343)
		List<ModuleDescriptor> parseResult = this.parser.parse(name, dslDefinition, module);

		ModuleType type = this.determineType(parseResult);
		assertModuleUpdatability(name, type, force);

		// TODO: XD-2284 need more than ModuleDefinitions (need to capture passed in options, etc)
		List<ModuleDefinition> composedModuleDefinitions = createComposedModuleDefinitions(parseResult);
		ModuleDefinition moduleDefinition = ModuleDefinitions.composed(name, type, dslDefinition, composedModuleDefinitions);

		Assert.isTrue(this.registry.registerNew(moduleDefinition), moduleDefinition + " could not be saved");
		return moduleDefinition;
	}

	public ModuleDefinition upload(String name, ModuleType type, byte[] bytes, boolean force) {
		assertModuleUpdatability(name, type, force);

		ModuleDefinition definition = new UploadedModuleDefinition(name, type, bytes);
		Assert.isTrue(this.registry.registerNew(definition), definition + " could not be saved");
		return definition;
	}

	/**
	 * Throws an exception if either one of the following is true:<ul>
	 *     <li>a module with the given name and type already exists and force is {@code false}</li>
	 *     <li>force if {@true} but the module is in use (by a stream or composed module)</li>
	 * </ul>
	 *
	 * <p>Also, will actually delete the already existing definition in case of an update,
	 * even though it would be overwritten by a registry, to cover the cases where a composed
	 * module is being replaced by a simple uploaded module, and <i>vice versa</i>.</p>
	 *
	 * @param name name of the module we're trying to update/create
	 * @param type type of the module we're trying to update/create
	 * @param force whether to attempt to force update if the module already exists
	 */
	private void assertModuleUpdatability(String name, ModuleType type, boolean force) {
		ModuleDefinition definition = registry.findDefinition(name, type);
		if (definition != null) {
			if (!force) {
				throw new ModuleAlreadyExistsException(name, type);
			} else {
				Set<String> dependents = this.dependencyRepository.find(name, type);
				if (!dependents.isEmpty()) {
					throw new DependencyException("Cannot force update module %2$s:%1$s because it is used by %3$s", name, type, dependents);
				} else {
					// Perform an eager deletion, taking care of module flavor change.
					// Also, this catches cases when we're trying to replace a read-only module
					if (!registry.delete(definition)) {
						throw new ModuleAlreadyExistsException("There is already a module named '%s' with type '%s', and it cannot be updated", name, type);
					}
				}
			}
		}
	}

	public void delete(String name, ModuleType type) {
		ModuleDefinition definition = registry.findDefinition(name, type);
		if (definition == null) {
			throw new NoSuchModuleException(name, type);
		}
		Set<String> dependents = this.dependencyRepository.find(name, type);
		if (!dependents.isEmpty()) {
			throw new DependencyException("Cannot delete module %2$s:%1$s because it is used by %3$s", name, type,
					dependents);
		}

		boolean result = this.registry.delete(definition);
		Assert.isTrue(result, String.format("Could not delete module '%s:%s'", type, name));
	}

	private List<ModuleDefinition> createComposedModuleDefinitions(
			List<ModuleDescriptor> moduleDescriptors) {

		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(moduleDescriptors.size());
		for (ModuleDescriptor moduleDescriptor : moduleDescriptors) {
			moduleDefinitions.add(registry.findDefinition(moduleDescriptor.getModuleName(),
					moduleDescriptor.getType()));
		}
		return moduleDefinitions;
	}

	private ModuleType determineType(List<ModuleDescriptor> modules) {
		Assert.isTrue(modules != null && modules.size() > 0, "at least one module required");
		if (modules.size() == 1) {
			return modules.get(0).getType();
		}
		Collections.sort(modules);
		ModuleType firstType = modules.get(0).getType();
		ModuleType lastType = modules.get(modules.size() - 1).getType();
		boolean hasInput = firstType != ModuleType.source;
		boolean hasOutput = lastType != ModuleType.sink;
		if (hasInput && hasOutput) {
			return ModuleType.processor;
		}
		if (hasInput) {
			return ModuleType.sink;
		}
		if (hasOutput) {
			return ModuleType.source;
		}
		throw new IllegalArgumentException("invalid module composition; must expose input and/or output channel");
	}
}
