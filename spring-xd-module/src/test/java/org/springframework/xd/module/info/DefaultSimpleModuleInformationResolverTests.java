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

package org.springframework.xd.module.info;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.xd.module.ModuleType.source;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;

/**
 * Unit tests for DefaultSimpleModuleInformationResolver.
 *
 * @author Eric Bottard
 */
public class DefaultSimpleModuleInformationResolverTests {

    @Test
    public void doesNotSupportComposedModules() {
        DefaultSimpleModuleInformationResolver resolver = new DefaultSimpleModuleInformationResolver();
        ModuleDefinition filter = ModuleDefinitions.simple("filter", ModuleType.processor, "classpath:dontcare");
        ModuleDefinition transform = ModuleDefinitions.simple("transform", ModuleType.processor, "classpath:dontcare");
        ModuleInformation result = resolver.resolve(ModuleDefinitions.composed("foo", ModuleType.processor, "filter | transform", Arrays.asList(filter, transform)));

        assertThat(result, CoreMatchers.nullValue());
    }

    @Test
    public void returnFound() {
        DefaultSimpleModuleInformationResolver resolver = new DefaultSimpleModuleInformationResolver();
        ModuleInformation result = resolver.resolve(craftDefinitionFor("module1", source));

        assertThat(result, notNullValue());
        assertThat(result.getShortDescription(), equalTo("I'm a cool module."));
    }

    @Test
    public void foundFileWithNoInfoIsOk() {
        DefaultSimpleModuleInformationResolver resolver = new DefaultSimpleModuleInformationResolver();
        ModuleInformation result = resolver.resolve(craftDefinitionFor("module2", source));

        assertThat(result, notNullValue());
        assertThat(result.getShortDescription(), nullValue());
    }

    private ModuleDefinition craftDefinitionFor(String name, ModuleType type) {
        String location = String.format("classpath:%s-modules/%s/%s/", DefaultSimpleModuleInformationResolverTests.class.getSimpleName(), type, name);
        return ModuleDefinitions.simple(name, type, location);
    }

}
