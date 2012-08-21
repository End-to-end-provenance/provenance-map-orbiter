/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2011
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

package edu.harvard.util.graph.summarizer;

import java.util.*;

import edu.harvard.util.Cancelable;
import edu.harvard.util.graph.*;
import edu.harvard.util.job.JobCanceledException;
import edu.harvard.util.job.JobObserver;


/**
 * A simple graph summarizer that arbitrarily breaks large summaries into small groups
 * 
 * @author Peter Macko
 */
public class SmallGroupsGraphSummarizer implements ObservableGraphSummarizer, Cancelable {

	private boolean canceled = false;
	private int nodeThreshold;
	private int edgeThreshold;
	
	
	/**
	 * Create an instance of class SmallGroupsGraphSummarizer
	 */
	public SmallGroupsGraphSummarizer() {
		nodeThreshold = 200;
		edgeThreshold = 300;
	}
	
	
	/**
	 * Recursively compute the graph summary
	 * 
	 * @param graph the graph
	 */
	public void summarize(BaseGraph graph) {
		summarize(graph, null);
	}
	
	
	/**
	 * Recursively compute the graph summary
	 * 
	 * @param graph the graph
	 * @param observer the observer
	 */
	public void summarize(BaseGraph graph, JobObserver observer) {
	
		canceled = false;
		graph.summarizationBegin();
		
		
		// For every summary node that currently exists...
		
		BaseSummaryNode root = graph.getRootBaseSummaryNode();
		Collection<BaseSummaryNode> summaryNodes = new HashSet<BaseSummaryNode>();
		root.collectBaseSummaryNodes(summaryNodes);
		
		if (observer != null) {
			if (summaryNodes.size() > 10) {
				observer.setRange(0, summaryNodes.size());
			}
			else {
				observer.makeIndeterminate();
			}
		}
		
		int count = 0;
		
		for (BaseSummaryNode sn : summaryNodes) {
			
			if (observer != null) {
				if (summaryNodes.size() > 10) {
					observer.setProgress(count);
				}
			}
			
			count++;
			

			if (sn.getBaseChildren().size() <= nodeThreshold) {
				if (sn.getInternalEdges().size() <= edgeThreshold) continue;
			}
			if (sn.getInternalEdges().isEmpty()) continue;
			
			ArrayList<BaseNode> nodes = new ArrayList<BaseNode>(sn.getBaseChildren().size());
			
			int groupsCreated = 1;
			int iterations = 0;
			while (groupsCreated > 0) {
				groupsCreated = 0;
				iterations++;
				
				if (iterations > 1000) break;
				
				if (sn.getBaseChildren().size() <= nodeThreshold) {
					if (sn.getInternalEdges().size() <= edgeThreshold) break;
				}
				if (sn.getInternalEdges().isEmpty()) break;
				
				int c = 0;
				for (BaseNode n : sn.getBaseChildren()) {
					if (n.isVisible()) c++;
				}
				if (c <= nodeThreshold) {
					if (sn.getInternalEdges().size() <= edgeThreshold) break;
				}
	
				nodes.clear();
				nodes.addAll(sn.getBaseChildren());
				
				
				// Compare every node with every other node
				
				for (int i = 0; i < nodes.size(); i++) {
					BaseNode pivot = nodes.get(i);
					
					if (!pivot.isVisible()) continue;

					groupsCreated++;
					BaseSummaryNode group = graph.newSummaryNode(sn);
					group.moveBaseNodeFromParent(pivot);
					
					if (canceled) throw new RuntimeException(new JobCanceledException());
					
					for (i = i + 1; i < nodes.size(); i++) {
						BaseNode n = nodes.get(i);
						if (n.getParent() != sn) continue;
						if (!n.isVisible()) continue;
						group.moveBaseNodeFromParent(n);
						if (group.getBaseChildren().size() >= nodeThreshold) break;
						if (group.getInternalEdges().size() >= edgeThreshold) break;
					}
				}
			}
		}
		
		
		// Finish
		
		graph.summarizationEnd();
	}

	
	/**
	 * Cancel all computations
	 */
	@Override
	public void cancel() {
		canceled = true;
	}
}
