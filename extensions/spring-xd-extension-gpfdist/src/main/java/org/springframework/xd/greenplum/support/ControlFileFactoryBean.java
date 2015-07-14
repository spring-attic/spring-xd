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

package org.springframework.xd.greenplum.support;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.xd.greenplum.support.ControlFile.OutputMode;

public class ControlFileFactoryBean implements FactoryBean<ControlFile>, InitializingBean {

	private ControlFile controlFile;

	private Resource controlFileResource;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (controlFileResource != null) {
			controlFile = parseYaml();
		}
		else {
			controlFile = new ControlFile();
		}
	}

	@Override
	public ControlFile getObject() throws Exception {
		return controlFile;
	}

	@Override
	public Class<ControlFile> getObjectType() {
		return ControlFile.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setControlFileResource(Resource controlFileResource) {
		this.controlFileResource = controlFileResource;
	}

	@SuppressWarnings({ "unchecked" })
	private ControlFile parseYaml() {
		ControlFile cf = new ControlFile();
		YamlMapFactoryBean factory = new YamlMapFactoryBean();
		factory.setResources(controlFileResource);
		Map<String, Object> yaml = factory.getObject();

		// main map
		for (Entry<String, Object> e0 : yaml.entrySet()) {
			if (e0.getKey().toLowerCase().equals("gpload")) {
				if (e0.getValue() instanceof Map) {
					Map<String, Object> gploadMap = (Map<String, Object>) e0.getValue();

					// GPLOAD map
					for (Entry<String, Object> e1 : gploadMap.entrySet()) {
						if (e1.getKey().toLowerCase().equals("output")) {

							// GPLOAD.OUTPUT list
							if (e1.getValue() instanceof List) {
								for (Object v : (List<?>) e1.getValue()) {
									if (v instanceof Map) {
										Map<String, Object> tableMap = (Map<String, Object>) v;
										for (Entry<String, Object> e2 : tableMap.entrySet()) {
											if (e2.getKey().toLowerCase().equals("table")) {
												if (e2.getValue() instanceof String) {
													cf.setGploadOutputTable((String) e2.getValue());
												}
											}
											else if (e2.getKey().toLowerCase().equals("mode")) {
												if (e2.getValue() instanceof String) {
													cf.setGploadOutputMode(OutputMode.valueOf(((String) e2.getValue()).toUpperCase()));
												}
											}
											else if (e2.getKey().toLowerCase().equals("match_columns")) {
												if (e2.getValue() instanceof List) {
													cf.setGploadOutputMatchColumns(((List<String>) e2.getValue()));
												}
											}
											else if (e2.getKey().toLowerCase().equals("update_columns")) {
												if (e2.getValue() instanceof List) {
													cf.setGploadOutputUpdateColumns(((List<String>) e2.getValue()));
												}
											}
											else if (e2.getKey().toLowerCase().equals("update_condition")) {
												if (e2.getValue() instanceof String) {
													cf.setGploadOutputUpdateCondition((String) e2.getValue());
												}
											}
										}

									}
								}
							}
						}
						else if (e1.getKey().toLowerCase().equals("input")) {
							if (e1.getValue() instanceof List) {
								for (Object v : (List<?>) e1.getValue()) {
									if (v instanceof Map) {
										Map<String, Object> tableMap = (Map<String, Object>) v;
										for (Entry<String, Object> e2 : tableMap.entrySet()) {
											if (e2.getKey().toLowerCase().equals("delimiter")) {
												if (e2.getValue() instanceof Character) {
													cf.setGploadInputDelimiter((Character) e2.getValue());
												}
												else if (e2.getValue() instanceof String) {
													if (((String) e2.getValue()).length() == 1) {
														cf.setGploadInputDelimiter(((String) e2.getValue()).charAt(0));
													}
												}
											}
										}
									}
								}
							}
						}
						else if (e1.getKey().toLowerCase().equals("sql")) {
							if (e1.getValue() instanceof List) {
								for (Object v : (List<?>) e1.getValue()) {
									if (v instanceof Map) {
										Map<String, Object> sqlMap = (Map<String, Object>) v;
										for (Entry<String, Object> e2 : sqlMap.entrySet()) {
											if (e2.getKey().toLowerCase().equals("before")) {
												if (e2.getValue() instanceof String) {
													cf.addGploadSqlBefore((String) e2.getValue());
												}
											}
											else if (e2.getKey().toLowerCase().equals("after")) {
												if (e2.getValue() instanceof String) {
													cf.addGploadSqlAfter((String) e2.getValue());
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			else if (e0.getKey().toLowerCase().equals("database")) {
				if (e0.getValue() instanceof String) {
					cf.setDatabase((String) e0.getValue());
				}
			}
			else if (e0.getKey().toLowerCase().equals("user")) {
				if (e0.getValue() instanceof String) {
					cf.setUser((String) e0.getValue());
				}
			}
			else if (e0.getKey().toLowerCase().equals("password")) {
				if (e0.getValue() instanceof String) {
					cf.setPassword((String) e0.getValue());
				}
			}
			else if (e0.getKey().toLowerCase().equals("host")) {
				if (e0.getValue() instanceof String) {
					cf.setHost((String) e0.getValue());
				}
			}
			else if (e0.getKey().toLowerCase().equals("port")) {
				if (e0.getValue() instanceof Integer) {
					cf.setPort((Integer) e0.getValue());
				}
			}
		}
		return cf;
	}

}
