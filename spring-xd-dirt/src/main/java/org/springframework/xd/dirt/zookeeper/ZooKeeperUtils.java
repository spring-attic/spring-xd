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

package org.springframework.xd.dirt.zookeeper;


import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;

import org.springframework.core.convert.converter.Converter;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.DeploymentLoader;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.MapBytesUtility;

/**
 * Utility methods for ZooKeeper.
 *
 * @author David Turanski
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 */
public abstract class ZooKeeperUtils {

	/**
	 * Utility to convert byte arrays to maps of strings.
	 */
	private static final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Utility for loading streams/jobs (including deployment metadata).
	 */
	private static final DeploymentLoader deploymentLoader = new DeploymentLoader();

	/**
	 * {@link Converter} from {@link ChildData} in stream deployments to Stream name.
	 *
	 * @see #streamDeployments
	 */
	public static final DeploymentNameConverter deploymentNameConverter = new DeploymentNameConverter();

	/**
	 * Converter from {@link ChildData} to deployment name string.
	 */
	public static class DeploymentNameConverter implements Converter<ChildData, String> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String convert(ChildData source) {
			return Paths.stripPath(source.getPath());
		}
	}

	/**
	 * Convert a map of string key/value pairs to a JSON string in a byte array.
	 *
	 * @param map map to convert
	 *
	 * @return byte array
	 */
	public static byte[] mapToBytes(Map<String, String> map) {
		return mapBytesUtility.toByteArray(map);
	}

	/**
	 * Convert a byte array containing a JSON string to a map of key/value pairs.
	 *
	 * @param bytes byte array containing the key/value pair strings
	 *
	 * @return a new map instance containing the key/value pairs
	 */
	public static Map<String, String> bytesToMap(byte[] bytes) {
		return mapBytesUtility.toMap(bytes);
	}

	/**
	 * Utility method to wrap a Throwable in a {@link ZooKeeperAccessException}.
	 * @param t the throwable
	 */
	public static RuntimeException wrapThrowable(Throwable t) {
		return wrapThrowable(t, null);
	}

	/**
	 * Utility method to wrap a Throwable in a {@link ZooKeeperAccessException}.
	 * @param t the throwable
	 * @param message use this message if not null
	 */
	public static RuntimeException wrapThrowable(Throwable t, String message) {
		if (message != null) {
			return new ZooKeeperAccessException(message, t);
		}
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		}
		else {
			return new ZooKeeperAccessException(t.getMessage(), t);
		}
	}

	/**
	 * Throw a wrapped exception ignoring specific Exception types, if any.
	 * @param t the throwable
	 * @param ignored a varargs list of ignored exception types
	 */
	@SuppressWarnings("unchecked")
	public static void wrapAndThrowIgnoring(Throwable t, Class... ignored) {
		if (ignored != null) {
			for (Class<? extends Exception> e : ignored) {
				if (e.isAssignableFrom(t.getClass())) {
					return;
				}
			}
		}
		throw wrapThrowable(t);
	}

	/**
	 * Load the {@link org.springframework.xd.dirt.core.Stream} instance
	 * for a given stream name <i>if the stream is deployed</i>. It will
	 * include the stream definition as well as any deployment properties
	 * data for the stream deployment.
	 *
	 * @param client         curator client
	 * @param streamName     the name of the stream to load
	 * @param streamFactory  stream factory used to create instance of stream
	 * @return the stream instance, or {@code null} if the stream does
	 *         not exist or is not deployed
	 * @throws Exception if ZooKeeper access fails for any reason
	 */
	public static Stream loadStream(CuratorFramework client, String streamName,
			StreamFactory streamFactory) throws Exception {
		return deploymentLoader.loadStream(client, streamName, streamFactory);
	}

	/**
	 * Load the {@link org.springframework.xd.dirt.core.Job}
	 * instance for a given job name<i>if the job is deployed</i>.
	 *
	 * @param client   curator client
	 * @param jobName  the name of the job to load
	 * @param jobFactory job factory used to create instance of job
	 * @return the job instance, or {@code null} if the job does not exist
	 *         or is not deployed
	 * @throws Exception
	 */
	public static Job loadJob(CuratorFramework client, String jobName,
			JobFactory jobFactory) throws Exception {
		return deploymentLoader.loadJob(client, jobName, jobFactory);
	}

	/**
	 * Utility method to log {@link org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent events}.
	 *
	 * @param logger logger to write to
	 * @param event  event to log
	 */
	public static void logCacheEvent(Logger logger, PathChildrenCacheEvent event) {
		ChildData data = event.getData();
		String path = (data == null) ? "null" : data.getPath();
		logger.info("Path cache event: {}, type: {}", path, event.getType());
		if (data != null && logger.isTraceEnabled()) {
			String content;
			byte[] bytes = data.getData();
			if (bytes == null || bytes.length == 0) {
				content = "empty";
			}
			else {
				try {
					content = new String(data.getData(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					content = "Could not convert content to UTF-8: " + e.toString();
				}
			}
			logger.trace("Data for path {}: {}", path, content);
		}
	}
}
