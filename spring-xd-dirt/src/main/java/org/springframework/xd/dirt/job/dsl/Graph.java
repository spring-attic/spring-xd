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

package org.springframework.xd.dirt.job.dsl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a Graph that Flo will display. A graph consists of simple nodes and links.
 *
 * @author Andy Clement
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Graph {

	public List<Node> nodes;

	public List<Link> links;

	public Map<String, String> properties;

	Graph() {
		this.nodes = new ArrayList<>();
		this.links = new ArrayList<>();
		this.properties = null;
	}

	Graph(List<Node> nodes, List<Link> links, Map<String, String> properties) {
		this.nodes = nodes;
		this.links = links;
		this.properties = (properties == null || properties.size() == 0) ? null : properties;
	}

	public List<Node> getNodes() {
		return this.nodes;
	}

	public List<Link> getLinks() {
		return this.links;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "Graph:  nodes=#" + nodes.size() + "  links=#" + links.size() + "\n" + nodes + "\n" + links;
	}

	public String toJSON() {
		Graph g = new Graph(nodes, links, properties);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			return mapper.writeValueAsString(g);
		}
		catch (IOException e) {
			throw new IllegalStateException("Unexpected problem creating JSON from Graph", e);
		}
	}

	/**
	 * Produce the DSL representation of the graph.
	 * To make this process easier we can assume there is a START and an END node.
	 *
	 * @return DSL string version of the graph
	 */
	public String toDSLText() {
		StringBuilder graphText = new StringBuilder();
		List<Node> unvisitedNodes = new ArrayList<>();
		List<Link> unfollowedLinks = new ArrayList<>();
		unvisitedNodes.addAll(nodes);
		unfollowedLinks.addAll(links);
		Node start = findNodeByName("START");
		unvisitedNodes.remove(start);
		Node end = findNodeByName("END");
		unvisitedNodes.remove(end);
		Node fail = findNodeByName("FAIL");
		if (fail != null) {
			unvisitedNodes.remove(fail);
		}
		if (start == null || end == null) {
			throw new IllegalStateException("Graph is malformed - problems finding START and END nodes");
		}
		List<Link> toFollow = findLinksFrom(start, false);
		followLinks(graphText, toFollow, null, unvisitedNodes, unfollowedLinks);

		if (unvisitedNodes.size() != 0) {
			int loopCount = 0;
			while (unvisitedNodes.size() != 0 && loopCount < 10000) {
				Node nextHead = findAHead(unvisitedNodes, unfollowedLinks);
				unvisitedNodes.remove(nextHead);
				toFollow = findLinksFrom(nextHead, false);
				// If the new head we find has no links to anything, we don't need to mention it in the DSL.
				// Transitions will refer to it and it will get a step in the XML but there is no need
				// to explicitly mention in the DSL. This might change once the job references support properties.
				if (toFollow.size() != 0) {
					graphText.append(" || ");
					printNode(graphText, nextHead, unvisitedNodes);
					followLinks(graphText, toFollow, null, unvisitedNodes, unfollowedLinks);
				}
				loopCount++;
			}
		}

		if (properties != null) {
			for (Map.Entry<String, String> property : properties.entrySet()) {
				graphText.append(" --").append(property.getKey()).append("=").append(property.getValue());
			}
		}

		return graphText.toString();
	}

	private Node findAHead(List<Node> unvisitedNodes, List<Link> unvisitedLinks) {
		if (unvisitedNodes.size() == 0) {
			return null;
		}
		Node candidate = unvisitedNodes.get(0);
		boolean changedCandidate = true;
		while (changedCandidate) {
			changedCandidate = false;
			for (Link link : unvisitedLinks) {
				if (link.to == candidate.id) {
					changedCandidate = true;
					candidate = findNodeById(link.from);
				}
			}
		}
		return candidate;
	}

	/**
	 * Chase down links, populating the graphText as it proceeds.
	 *
	 * @param graphText where to place the DSL text as we process the graph
	 * @param toFollow the links to follow
	 * @param nodeToTerminateFollow the node that should trigger termination of following
	 */
	private void followLinks(StringBuilder graphText, List<Link> toFollow, Node nodeToTerminateFollow,
			List<Node> unvisitedNodes, List<Link> unfollowedLinks) {
		while (toFollow.size() != 0) {
			if (toFollow.size() > 1) { // SPLIT
				if (graphText.length() != 0) {
					// If there is something already in the text, a || is needed to
					// join it to the preceding element
					graphText.append(" || ");
				}
				graphText.append("<");
				Node endOfSplit = findEndOfSplit(toFollow);
				for (int i = 0; i < toFollow.size(); i++) {
					if (i > 0) {
						graphText.append(" & ");
					}
					Link l = toFollow.get(i);
					followLink(graphText, l, endOfSplit, unvisitedNodes, unfollowedLinks);
				}
				graphText.append(">");
				if (endOfSplit == null || endOfSplit.isEnd()) {
					// nothing left to do
					break;
				}
				if (endOfSplit == nodeToTerminateFollow) {
					// Time to finish if termination node hit
					break;
				}
				unvisitedNodes.remove(endOfSplit);
				if (!endOfSplit.isSync()) {
					// If not a sync node, include it in the output text
					graphText.append(" || ");
					printNode(graphText, endOfSplit, unvisitedNodes);
					List<Link> transitionalLinks = findLinksFrom(endOfSplit, false);
					printTransitions(graphText, unfollowedLinks, transitionalLinks);
				}
				toFollow = findLinksFromWithoutTransitions(endOfSplit, false);
			}
			else if (toFollow.size() == 1) { // FLOW
				Link linkToFollow = toFollow.get(0);
				Node linkToFollowTarget = findNodeById(linkToFollow.to);
				if (linkToFollowTarget != nodeToTerminateFollow) {
					if (linkToFollowTarget.isEnd() || linkToFollowTarget.isFail()) {
						unfollowedLinks.remove(linkToFollow);
					}
					else {
						if (graphText.length() != 0) {
							// First one doesn't need a || on the front
							graphText.append(" || ");
						}
						followLink(graphText, linkToFollow, nodeToTerminateFollow, unvisitedNodes, unfollowedLinks);
					}
				}
				break;
			}
		}
	}

	private Node findEndOfSplit(List<Link> toFollow) {
		if (toFollow.size() == 0) {
			return null;
		}
		if (toFollow.size() == 1) {
			// return the first node...
			return findNodeById(toFollow.get(0).to);
		}
		// Follow the first link. For each node found see if it
		// exists down the chain of all the other links (i.e. is a common target)
		Link link = toFollow.get(0);
		Node nextCandidate = findNodeById(link.to);
		while (nextCandidate != null) {
			boolean allLinksLeadToTheCandidate = true;
			for (int l = 1; l < toFollow.size(); l++) {
				if (!foundInChain(toFollow.get(l), nextCandidate)) {
					allLinksLeadToTheCandidate = false;
					break;
				}
			}
			if (allLinksLeadToTheCandidate) {
				return nextCandidate;
			}
			List<Link> links = findLinksFromWithoutTransitions(nextCandidate, true);
			if (links.size() == 0) {
				nextCandidate = null;
			}
			else if (links.size() == 1) {
				nextCandidate = findNodeById(links.get(0).to);
			}
			else {
				while (countLinksWithoutTransitions(links) > 1) {
					nextCandidate = findEndOfSplit(links);
					links = findLinksFromWithoutTransitions(nextCandidate, true);
				}
			}
		}
		// This indicates a broken graph
		return null;
	}

	/**
	 * Walk a specified link to see if it ever hits the candidate node.
	 *
	 * @param link points to the head of a chain of nodes
	 * @param candidate the node possibly found on the chain of nodes
	 * @return true if the candidate is found down the specified chain
	 */
	private boolean foundInChain(Link link, Node candidate) {
		String targetId = link.to;
		Node targetNode = findNodeById(targetId);
		if (targetNode == candidate) {
			return true;
		}
		List<Link> outboundLinks = findLinksFromWithoutTransitions(targetNode, true);
		while (outboundLinks.size() > 0) {
			while (countLinksWithoutTransitions(outboundLinks) > 1) {
				Node splitEnd = findEndOfSplit(outboundLinks);
				// eee is found as the split end for ccc&ddd  - however, it is ALSO the end of the outer split
				if (splitEnd == candidate) {
					return true;
				}
				outboundLinks = findLinksFromWithoutTransitions(splitEnd, true);
			}
			// Now we are at a single link (or no links) in outbound links
			if (outboundLinks.size() > 0) {
				// single link
				targetNode = findNodeById(outboundLinks.get(0).to);
				if (targetNode == candidate) {
					return true;
				}
				outboundLinks = findLinksFromWithoutTransitions(targetNode, true);
			}
		}
		return false;
	}

	private int countLinksWithoutTransitions(List<Link> links) {
		int count = 0;
		for (Link link : links) {
			if (!link.hasTransitionSet()) {
				count++;
			}
		}
		return count;
	}

	private void printNode(StringBuilder graphText, Node node, List<Node> unvisitedNodes) {
		unvisitedNodes.remove(node);
		// What to generate depends on whether it is a job definition or reference
		if (node.metadata != null && node.metadata.containsKey(Node.METADATAKEY_JOBMODULENAME)) {
			graphText.append(node.metadata.get(Node.METADATAKEY_JOBMODULENAME)).append(" ");
			graphText.append(node.name).append(" ");
			if (node.properties != null) {
				int count = 0;
				for (Map.Entry<String, String> entry : node.properties.entrySet()) {
					if (count > 0) {
						graphText.append(" ");
					}
					graphText.append("--").append(entry.getKey()).append("=").append(entry.getValue());
					count++;
				}
			}
		}
		else {
			String nameInDSL = node.name;
			graphText.append(nameInDSL);
			if (node.properties != null) {
				for (Map.Entry<String, String> entry : node.properties.entrySet()) {
					graphText.append(" ");
					graphText.append("--").append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
	}

	private void followLink(StringBuilder graphText, Link link, Node nodeToFinishFollowingAt,
			List<Node> unvisitedNodes, List<Link> unfollowedLinks) {
		unfollowedLinks.remove(link);
		Node target = findNodeById(link.to);
		printNode(graphText, target, unvisitedNodes);
		List<Link> toFollow = findLinksFrom(target, false);
		printTransitions(graphText, unfollowedLinks, toFollow);

		// if there isn't a node to finish following at and to follow is empty - look for the next 'head' ?
		//		if (nodeToFinishFollowingAt == null && toFollow.size() == 0 && unvisitedNodes.size() != 0) {
		//			// Find another head to follow. We will join it via '||' which is not right but it is all we have...
		//			// I'm asserting it will be OK because it won't be confusing or misrepresent the actual definition.
		//			// What else can we assert here? Assert the previous node had 'all exits covered' ?
		//			int stop = 1;
		//			findNexth
		//		}

		followLinks(graphText, toFollow, nodeToFinishFollowingAt, unvisitedNodes, unfollowedLinks);
	}

	private void printTransitions(StringBuilder graphText, List<Link> unfollowedLinks, List<Link> toFollow) {
		for (Iterator<Link> iterator = toFollow.iterator(); iterator.hasNext();) {
			Link l = iterator.next();
			if (l.hasTransitionSet()) {
				// capture the target of this link as a simple transition
				String transitionName = l.getTransitionName();
				Node transitionTarget = findNodeById(l.to);
				String transitionTargetName = transitionTarget.name;
				if (transitionTargetName.equals("FAIL")) {
					transitionTargetName = Transition.FAIL;
				}
				else if (transitionTargetName.equals("END")) {
					transitionTargetName = Transition.END;
				}
				graphText.append(" | ").append(transitionName).append(" = ").append(transitionTargetName);
				unfollowedLinks.remove(l);
				iterator.remove();
			}
		}
	}

	private Node findNodeById(String id) {
		for (Node n : nodes) {
			if (n.id.equals(id)) {
				return n;
			}
		}
		return null;
	}

	private Node findNodeByName(String name) {
		for (Node n : nodes) {
			if (n.name.equals(name)) {
				return n;
			}
		}
		return null;
	}

	private List<Link> findLinksFromWithoutTransitions(Node n, boolean includeThoseLeadingToEnd) {
		List<Link> result = new ArrayList<>();
		for (Link link : links) {
			if (link.from.equals(n.id)) {
				if (!link.hasTransitionSet()
						&& (includeThoseLeadingToEnd || !findNodeById(link.to).name.equals("END"))) {
					result.add(link);
				}
			}
		}
		return result;
	}

	private List<Link> findLinksFrom(Node n, boolean includeThoseLeadingToEnd) {
		List<Link> result = new ArrayList<>();
		for (Link link : links) {
			if (link.from.equals(n.id)) {
				// Only include links to 'END' if there are properties on it
				if (includeThoseLeadingToEnd || !(findNodeById(link.to).name.equals("END") && link.properties == null)) {
					result.add(link);
				}
			}
		}
		return result;
	}
}
