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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;

/**
 * Unit tests for DelegatingModuleInformationResolver.
 *
 * @author Eric Bottard
 */
public class DelegatingModuleInformationResolverTests {

    @Test
    public void firstNonNullWins() {
        ModuleInformation info1 = new ModuleInformation();
        ModuleInformation info2 = new ModuleInformation();
        ModuleInformationResolver resolver1 = Mockito.mock(ModuleInformationResolver.class);
        Mockito.when(resolver1.resolve(Mockito.any(ModuleDefinition.class))).thenReturn(info1);
        ModuleInformationResolver resolver2 = Mockito.mock(ModuleInformationResolver.class);
        Mockito.when(resolver2.resolve(Mockito.any(ModuleDefinition.class))).thenReturn(info2);

        DelegatingModuleInformationResolver resolver = new DelegatingModuleInformationResolver();
        resolver.setDelegates(Arrays.asList(resolver1, resolver2));

        ModuleInformation result = resolver.resolve(ModuleDefinitions.simple("foo", ModuleType.processor, "classpath:dontcare"));
        assertThat(result, CoreMatchers.equalTo(info1));

    }

    @Test
    public void nullResultsCarryThrough() {
        ModuleInformation info1 = new ModuleInformation();
        ModuleInformation info2 = new ModuleInformation();
        ModuleInformationResolver resolver1 = Mockito.mock(ModuleInformationResolver.class);
        Mockito.when(resolver1.resolve(ModuleDefinitions.simple("bar", ModuleType.processor, "classpath:dontcare"))).thenReturn(info1);
        ModuleInformationResolver resolver2 = Mockito.mock(ModuleInformationResolver.class);
        Mockito.when(resolver2.resolve(ModuleDefinitions.simple("wizz", ModuleType.processor, "classpath:dontcare"))).thenReturn(info2);

        DelegatingModuleInformationResolver resolver = new DelegatingModuleInformationResolver();
        resolver.setDelegates(Arrays.asList(resolver1, resolver2));

        ModuleInformation result = resolver.resolve(ModuleDefinitions.simple("foo", ModuleType.processor, "classpath"));
        assertThat(result, CoreMatchers.nullValue());

    }

}
