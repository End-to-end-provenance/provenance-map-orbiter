/*
 * Provenance Aware Storage System - Java Utilities
 *
 * Copyright 2012
 *      The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package edu.harvard.pass.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.harvard.pass.PGraph;
import edu.harvard.pass.PGraphStat;
import edu.harvard.pass.PNode;
import edu.harvard.pass.PObject.Type;
import edu.harvard.pass.PSummaryNode;
import edu.harvard.util.Utils;
import edu.harvard.util.graph.BaseGraph;
import edu.harvard.util.graph.BaseNode;
import edu.harvard.util.graph.summarizer.GraphSummarizer;
import edu.harvard.util.graph.summarizer.SummarizerUtils;


/**
 * Summarize by time.
 * 
 * Algorithm originally designed by: Madelaine Boyd
 * 
 * @author Peter Macko
 */
public class TimestampsSummarizer implements GraphSummarizer {
	
	protected boolean processesOnly = false;
	protected boolean useAdjustedTime = true;
	protected boolean keepVersionsTogether = true;
	protected boolean doNodesWithoutTimestampLater = false;
	protected boolean useBreakTimes = true;
	
	
	/**
	 * Get a time of the given node
	 * 
	 * @param node the node
	 * @return the time
	 */
	private double getTime(PNode node) {
		return useAdjustedTime ? node.getTime() : node.getTimeUnadjusted();
	}

	
	/**
	 * Compute a base threshold
	 * 
	 * @param graph the graph
	 * @return the base threshold
	 */
	private double computeBaseThreshold(PGraph graph) {
		
		PGraphStat stat = graph.getStat();
		if (graph.getNodes().isEmpty()) return 1;
		
		double maxTime = stat.getTimeUnadjustedMax() - (useAdjustedTime ? graph.getTimeBase() : 0);
		return maxTime / graph.getNodes().size();
	}
	
	
	/**
	 * Count the number of clusters based on the given threshold
	 * 
	 * @param nodes the list of nodes sorted by time
	 * @param baseThreshold the base threshold
	 * @param thresholdMultiplier the threshold multiplier
	 * @return the number of clusters
	 */
	private int computeClusterCount(List<PNode> nodes, double baseThreshold, double thresholdMultiplier) {
		
		double threshold = thresholdMultiplier * baseThreshold;
		if (nodes.isEmpty()) return 0;
		
		double lastTime = getTime(nodes.get(0));
		int numClusters = 1;
		double maxDT = 0;
		
		for (PNode n : nodes) {
			double time = getTime(n);
			double dt = time - lastTime;
			if (dt > threshold) {
				lastTime = n.getAux();
				numClusters++;
			}
			if (dt > maxDT) maxDT = dt;
			if (useBreakTimes) lastTime = n.getAux();
		}
		
		return numClusters;
	}
	
	
	/**
	 * Get a suitable threshold multiplier
	 * 
	 * @param nodes the list of nodes sorted by time
	 * @param baseThreshold the base threshold
	 * @param thresholdMultiplier the initial threshold multiplier
	 * @param minClusters the minimum desired number of clusters
	 * @param maxClusters the maximum desired number of clusters
	 * @return a suitable threshold multiplier
	 */
	private double computeSuitableThresholdMultiplier(List<PNode> nodes, double baseThreshold,
			double thresholdMultiplier, int minClusters, int maxClusters) {
		
		double m = thresholdMultiplier;
		
		for (int retry = 0; retry < 20; retry++) {
			int count = computeClusterCount(nodes, baseThreshold, m);
			if (count < minClusters) m *= 2;
			else if (count > maxClusters) m *= 0.5;
			else return m;
		}
		
		int count = computeClusterCount(nodes, baseThreshold, m);
		if (count > maxClusters) {
			System.err.println("Warning: TimestampsSummarizer.computeSuitableThresholdMultiplier() "
					+ "reached the retry limit; count = " + count);
		}
		
		return m;
	}
	
	
	/**
	 * Recursively compute the graph summary for the given summary node
	 * 
	 * @param graph the graph
	 * @param summary the summary node
	 * @param baseThreshold the base threshold
	 * @param minNodesPerCluster the minimum desired number of nodes per cluster
	 * @param maxNodesPerCluster the maximum desired number of nodes per cluster
	 */
	private void summarize(PGraph graph, PSummaryNode summary, double baseThreshold,
			int minNodesPerCluster, int maxNodesPerCluster) {
		
		
		// Get the nodes and sort them by time
		
		ArrayList<PNode> nodesToWorkOn = new ArrayList<PNode>();
		for (BaseNode b : summary.getBaseChildren()) {
			if (b instanceof PNode) {
				PNode n = (PNode) b;
				if (processesOnly) {
					if (n.getObject().getType() != Type.PROCESS) continue;
				}
				if (doNodesWithoutTimestampLater) {
					if (n.getTime() == 0) continue;
				}
				nodesToWorkOn.add(n);
			}
		}
		
		ArrayList<PNode> nodes = new ArrayList<PNode>();
		for (PNode n : nodesToWorkOn) {
			if (keepVersionsTogether) {
				if (n.getPrev() != null) continue;
			}
			for (PNode latest = n; latest != null; latest = latest.getNext()) n.setAux(getTime(latest));
			nodes.add(n);
		}
		
		if (nodes.isEmpty()) return;
		Collections.sort(nodes, new Comparator<PNode>() {
			@Override
			public int compare(PNode n1, PNode n2) {
				double t1 = getTime(n1);
				double t2 = getTime(n2);
				if (t1 < t2) return -1;
				if (t1 > t2) return  1;
				return n1.compareTo(n2);
			}
		});
		
		
		// Get the threshold multiplier and compute the threshold
		
		double m = computeSuitableThresholdMultiplier(nodes, baseThreshold, 3,
				minNodesPerCluster, maxNodesPerCluster);
		double threshold = m * baseThreshold;
		
		
		// Cluster the summary nodes
		
		double lastTime = getTime(nodes.get(0));
		PSummaryNode s = graph.newSummaryNode(summary);
		List<PSummaryNode> summaries = new ArrayList<PSummaryNode>();
		summaries.add(s);
		
		for (PNode n : nodes) {
			
			double time = getTime(n);
			double dt = time - lastTime;
			if (useBreakTimes) lastTime = n.getAux();
			
			if (dt > threshold) {
				s = summary.getGraph().newSummaryNode(summary);
				summaries.add(s);
				lastTime = n.getAux();
			}
			
			s.moveBaseNodeFromParent(n);		
		}
		
		
		// Move each subsequent version of each node to the appropriate summary node
		
		if (keepVersionsTogether) {
			for (PNode n : nodesToWorkOn) {
				if (doNodesWithoutTimestampLater) {
					if (n.getTime() == 0) continue;
				}
				if (n.getPrev() != null) {
					PNode first = null;
					for (PNode f = n.getPrev(); f != null; f = f.getPrev()) {
						first = f;
					}
					if (first == null) throw new InternalError();
					first.getParent().moveBaseNodeFromAncestor(n);
				}
			}
		}

		
		// Name each new summary node
		
		for (PSummaryNode si : summaries) {
			
			PNode node = null;
			int maxOutDegree = -1;
			
			for (BaseNode b : si.getBaseChildren()) {
				if (b instanceof PNode) {
					PNode n = (PNode) b;
					int outDegree = n.getOutgoingBaseEdges().size();
					if (outDegree > maxOutDegree) {
						maxOutDegree = outDegree;
						node = n;
					}
				}
			}
			
			if (node != null) {
				si.setLabel(node.getLabel());
			}
		}
		
		
		// If each of the clusters is too big, summarize it
		
		if (summaries.size() > 1) {
			for (PSummaryNode si : summaries) {
				if (si.getBaseChildren().size() > maxNodesPerCluster) {
					summarize(graph, si, baseThreshold, minNodesPerCluster, maxNodesPerCluster);
				}
			}
		}
	}

	
	/**
	 * Recursively compute the graph summary
	 * 
	 * @param graph the graph
	 */
	public void summarize(BaseGraph graph) {

		if (!(graph instanceof PGraph)) {
			throw new IllegalArgumentException("The graph needs to be an instance of PGraph");
		}
		
		PGraph g = Utils.<PGraph>cast(graph);
		
		
		// Initialize
		
		g.summarizationBegin();
		PSummaryNode root = g.getRootSummaryNode();
		double baseThreshold = computeBaseThreshold(g);
		
		
		// Summarize the graph
		
		summarize(g, root, baseThreshold, 5, 60);
		SummarizerUtils.assignUnassigned(g);
		
		
		// Finalize
		
		SummarizerUtils.removeSingletonSummaries(g);
		g.summarizationEnd();
	}
	
	
	/**
	 * The processes-only variant of the summarizer
	 * 
	 * @author Peter Macko
	 */
	public static class ProcessesOnly extends TimestampsSummarizer {
		
		/**
		 * Create an instance of class ProcessesOnly
		 */
		public ProcessesOnly() {
			processesOnly = true;
		}
	}
}
