/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.x.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.util.ClassUtils;


/**
 * A registry of {@link HttpMessageConverter}s that will attempt each one in turn until one succeeds.
 * 
 * @author Eric Bottard
 */
public class DelegatingHttpMessageConverter {

	private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			DelegatingHttpMessageConverter.class.getClassLoader());

	private static boolean romePresent = ClassUtils.isPresent("com.sun.syndication.feed.WireFeed",
			DelegatingHttpMessageConverter.class.getClassLoader());

	private List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

	public DelegatingHttpMessageConverter() {
		this(true);
	}

	public DelegatingHttpMessageConverter(boolean installDefaultConverters) {
		if (installDefaultConverters) {
			installDefaultConverters();
		}
	}

	public <T> T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		MediaType contentType = inputMessage.getHeaders().getContentType();

		for (HttpMessageConverter<?> converter : converters) {
			if (converter.canRead(clazz, contentType)) {
				@SuppressWarnings("unchecked")
				HttpMessageConverter<T> c = (HttpMessageConverter<T>) converter;
				return c.read(clazz, inputMessage);
			}
		}
		return null;

	}


	public <T> void write(T t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		for (HttpMessageConverter<?> messageConverter : converters) {
			Class<?> clazz = t.getClass();
			if (messageConverter.canWrite(clazz, contentType)) {
				@SuppressWarnings("unchecked")
				HttpMessageConverter<T> cast = (HttpMessageConverter<T>) messageConverter;
				cast.write(t, contentType, outputMessage);
				return;
			}
		}

	}

	private void installDefaultConverters() {
		// converters.add(new MultipartAwareFormHttpMessageConverter());
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new SourceHttpMessageConverter());
		if (jaxb2Present) {
			converters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (JacksonJsonUtils.isJackson2Present()) {
			converters.add(new MappingJackson2HttpMessageConverter());
		}
		if (romePresent) {
			// converters.add(new AtomFeedHttpMessageConverter());
			// converters.add(new RssChannelHttpMessageConverter());
		}
	}


}
