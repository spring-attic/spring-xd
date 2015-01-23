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


package org.springframework.integration.x.kafka;

import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

/**
 * Wraps a delegate {@link MetadataStore} and adds a prefix to all the handled keys.
 *
 * Useful for sharing the same {@link MetadataStore} or underlying location without interference
 * between clients.
 *
 * @author Marius Bogoevici
 */
public class NamespaceDelegatingMetadataStore implements MetadataStore {

	private final MetadataStore delegate;

	private final String namespace;

	public NamespaceDelegatingMetadataStore(MetadataStore delegate, String namespace) {
		this.delegate = delegate;
		this.namespace = namespace;
	}

	@Override
	public void put(String key, String value) {
		delegate.put(getDelegateKey(key), value);
	}

	@Override
	@ManagedAttribute
	public String get(String key) {
		return delegate.get(getDelegateKey(key));
	}

	@Override
	@ManagedAttribute
	public String remove(String key) {
		return delegate.remove(getDelegateKey(key));
	}

	private String getDelegateKey(String key) {
		Assert.notNull(key, "cannot be null");
		return namespace + ":" + key;
	}
}
