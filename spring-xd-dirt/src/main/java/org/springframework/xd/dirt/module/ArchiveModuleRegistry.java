/*
 * Copyright 2011-2014 the original author or authors.
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
package org.springframework.xd.dirt.module;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A ModuleRegistry that expects to find Spring Boot archives (either as jar file or exploded directory)
 * at the following location pattern: {@code <registry root>/<module type>/<archive named after the module>}.
 *
 * @author Eric Bottard
 */
public class ArchiveModuleRegistry implements ModuleRegistry, ResourceLoaderAware {

    private final static String[] SUFFIXES = new String[]{"", ".jar"};

    private String root;
    private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver() ;

    public ArchiveModuleRegistry(String root) {
        this.root = StringUtils.trimTrailingCharacter(root, '/');
    }

    @Override
    public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
        String location = String.format("%s/%s/%s", root, moduleType.name(), name);
        Resource resource = resolver.getResource(location);
        return resource == null ? null : new SimpleModuleDefinition(name, moduleType, location);
    }

    @Override
    public List<ModuleDefinition> findDefinitions(String name) {
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
        try {
            for (String suffix : SUFFIXES) {
                Resource[] resources = resolver.getResources(String.format("%s/*/%s%s", root, name, suffix));
                for (Resource resource : resources) {
                    result.add(fromResource(resource));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Override
    public List<ModuleDefinition> findDefinitions(ModuleType type) {
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
        try {
            Resource[] resources = resolver.getResources(String.format("%s/%s/*", root, type.name()));
            for (Resource resource : resources) {
                result.add(fromResource(resource));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Override
    public List<ModuleDefinition> findDefinitions() {
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
        try {
            Resource[] resources = resolver.getResources(String.format("%s/*/*", root));
            for (Resource resource : resources) {
                result.add(fromResource(resource));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    private SimpleModuleDefinition fromResource(Resource resource) {
        try {
            boolean isDir = resource.getFile().isDirectory();
            String filename = resource.getFilename();
            String name = isDir ? filename : filename.substring(0, filename.lastIndexOf('.'));
            String canonicalPath = resource.getFile().getCanonicalPath();
            int lastSlash = canonicalPath.lastIndexOf('/');
            String typeAsString = canonicalPath.substring(canonicalPath.lastIndexOf('/', lastSlash - 1), lastSlash);
            return new SimpleModuleDefinition(name, ModuleType.valueOf(typeAsString), "file:" + canonicalPath);
        } catch (IOException e) {
            throw new IllegalStateException("ArchiveModuleRegistry assumes resource can be resolved to a java.io.File: " + resource, e);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        Assert.isTrue(resourceLoader instanceof ResourcePatternResolver,
                "resourceLoader must be a ResourcePatternResolver");
        resolver = (ResourcePatternResolver) resourceLoader;
    }

}
