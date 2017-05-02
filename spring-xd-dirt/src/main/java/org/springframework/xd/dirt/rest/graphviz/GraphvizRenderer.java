/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest.graphviz;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.XDParser;


/**
 * A quick and dirty way to export stream definitions as a dot graph.
 * 
 * @author Eric Bottard
 */
@Controller
public class GraphvizRenderer {

	@Autowired
	private XDParser parser;

	@Autowired
	private StreamDefinitionRepository repository;

	@ResponseBody
	@RequestMapping(value = "/streams", method = RequestMethod.GET, produces = "text/vnd.graphviz")
	@ResponseStatus(HttpStatus.OK)
	public void graphviz(Writer out) throws IOException {
		Map<String, List<ModuleDeploymentRequest>> allStreams = new HashMap<String, List<ModuleDeploymentRequest>>();
		for (StreamDefinition stream : repository.findAll()) {
			List<ModuleDeploymentRequest> modules = parser.parse(stream.getName(), stream.getDefinition());
			Collections.reverse(modules);
			allStreams.put(stream.getName(), modules);
		}

		GraphvizWriter graphviz = new GraphvizWriter(out);
		graphviz.open("digraph G");
		graphviz.line("graph [rankdir = LR, splines = ortho]");
		for (String name : allStreams.keySet()) {
			renderStream(graphviz, name, allStreams);
		}
		graphviz.close();
		out.close();
	}

	private void renderStream(GraphvizWriter graphviz, String name,
			Map<String, List<ModuleDeploymentRequest>> allStreams)
			throws IOException {
		Map<String, String> channelVertexToLabel = new HashMap<String, String>();

		graphviz.open("subgraph cluster_%s", name);
		graphviz.line("label = \"%s\"", name);

		List<ModuleDeploymentRequest> modules = allStreams.get(name);
		ModuleDeploymentRequest previous = null;
		for (ModuleDeploymentRequest module : modules) {
			if (previous != null) {
				graphviz.line("%s -> %s", makeVertexName(previous), makeVertexName(module));
			}
			graphviz.line("%s", makeVertexName(module));
			graphviz.line("%s [label=\"%s\"]", makeVertexName(module), makeModuleLabel(module));
			graphviz.line("%s [shape=box, style=rounded]", makeVertexName(module));

			String sourceChannelName = module.getSourceChannelName();
			String sinkChannelName = module.getSinkChannelName();
			if (sourceChannelName != null) {
				String from = makeVertexNameFromChannel(sourceChannelName, allStreams);
				if (!isTap(sourceChannelName)) {
					channelVertexToLabel.put(from, sourceChannelName);
				}
				graphviz.line("%s -> %s [style = dashed]", from, makeVertexName(module));
			}
			if (sinkChannelName != null) {
				String to = makeVertexNameFromChannel(sinkChannelName, allStreams);
				if (!isTap(sinkChannelName)) {
					channelVertexToLabel.put(to, sinkChannelName);
				}
				graphviz.line("%s -> %s [style = dashed]", makeVertexName(module), to);
			}
			previous = module;
		}

		graphviz.close();

		// Emit channels a edges out of any stream
		for (Map.Entry<String, String> entry : channelVertexToLabel.entrySet()) {
			graphviz.line("%s [label=\"%s\"]", entry.getKey(), ":" + entry.getValue());
		}
	}

	private String makeModuleLabel(ModuleDeploymentRequest module) {
		StringBuilder sb = new StringBuilder(module.getModule());
		for (String key : module.getParameters().keySet()) {
			sb.append("\\n").append(key).append(" = ").append(module.getParameters().get(key));
		}
		return sb.toString();
	}

	private String makeVertexNameFromChannel(String channel, Map<String, List<ModuleDeploymentRequest>> allStreams) {
		int dot = channel.lastIndexOf('.');
		int colon = channel.indexOf(':');
		if (isTap(channel)) {
			String streamName = channel.substring(colon + 1, dot);
			String moduleName = channel.substring(dot + 1);
			ModuleDeploymentRequest module = locateFirstModuleMatching(streamName, moduleName,
					allStreams.get(streamName));
			return makeVertexName(module);
		}
		else {
			return "colon_" + channel;
		}
	}

	/**
	 * A tap module name does not know about the module index. Recover it from the stream definition.
	 */
	private ModuleDeploymentRequest locateFirstModuleMatching(String streamName, String moduleName,
			List<ModuleDeploymentRequest> modules) {
		for (ModuleDeploymentRequest m : modules) {
			if (m.getModule().equals(moduleName)) {
				return m;
			}
		}
		throw new IllegalStateException();
	}

	private boolean isTap(String channel) {
		return channel.startsWith("tap:");
	}

	private String makeVertexName(ModuleDeploymentRequest module) {
		return module.getGroup() + "_" + module.getModule().replaceAll("-", "_dash_") + "_" + module.getIndex();
	}

}
