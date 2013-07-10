/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.x.channel.registry;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

/**
 * @author David Turanski
 *
 */
public abstract class AbstractChannelRegistry implements ChannelRegistry {
	private Map<MediaType, AbstractPayloadTransformer<?, ?>> payloadTransformers;
	private AbstractPayloadTransformer<?, ?> defaultPayloadTransformer;
	protected Log logger = LogFactory.getLog(getClass());

	public void setPayloadTransformers(Map<MediaType, AbstractPayloadTransformer<?, ?>> payloadTransformers) {
		this.payloadTransformers = payloadTransformers;
	}

	public void setDefaultPayloadTransformer(AbstractPayloadTransformer<?, ?> payloadTransformer) {
		this.defaultPayloadTransformer = payloadTransformer;
	}

	/**
	 * @return the payloadTransformers
	 */
	protected AbstractPayloadTransformer<?, ?> getPayloadTransformer(MediaType mediaType) {
		return payloadTransformers == null ? null : payloadTransformers.get(mediaType);
	}

	/**
	 * @return the defaultPayloadTransformer
	 */
	protected AbstractPayloadTransformer<?, ?> getDefaultPayloadTransformer() {
		return defaultPayloadTransformer;
	}

	protected Map<MediaType, AbstractPayloadTransformer<?, ?>> getPayloadTransformers() {
		return payloadTransformers;
	}

	/**
	 * Resolve the payload transformer to use based on message content. If a transformer is configured for the 
	 * {@link MediaType} matching the content type message header, use that. If none is configured, see if there is 
	 * a transformer mapped to MediaType.ALL. Otherwise fall back to the defaultPayloadTransformer.
	 * @param message the {@link Message}
	 * @return the {@link AbstractPayloadTransformer} for this message or null if none is configured
	 */

	protected AbstractPayloadTransformer<?, ?> resolvePayloadTransformerForMessage(Message<?> message) {
		AbstractPayloadTransformer<?, ?> payloadTransformer = getDefaultPayloadTransformer();
		if (this.payloadTransformers != null) {
			String contentType = (String) message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
			if (contentType != null) {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (getPayloadTransformer(mediaType) != null) {
					payloadTransformer = getPayloadTransformer(mediaType);
				} else if (getPayloadTransformer(MediaType.ALL) != null) {
					payloadTransformer = getPayloadTransformer(MediaType.ALL);
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Using payload transformer ["
					+ (payloadTransformer == null ? "none" : payloadTransformer.getClass().getName()) + "]");
		}
		return payloadTransformer;
	}
}
