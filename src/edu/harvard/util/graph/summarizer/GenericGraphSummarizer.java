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
 * A generic group builder: robust, but slow -- on the order of O(n^2) per pass
 * 
 * @author Peter Macko
 */
public class GenericGraphSummarizer implements ObservableGraphSummarizer, Cancelable {

	private BaseSummaryChecker checker;
	private boolean canceled = false;
	
	
	/**
	 * Create an instance of class GenericGraphSummarizer
	 * 
	 * @param checker the group checker
	 */
	public GenericGraphSummarizer(BaseSummaryChecker checker) {
		this.checker = checker;
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
			
			ArrayList<BaseNode> nodes = new ArrayList<BaseNode>(sn.getBaseChildren().size());
			
			if (observer != null) {
				if (summaryNodes.size() > 10) {
					observer.setProgress(count);
				}
			}
			
			count++;
			
			int groupsCreated = 1;
			while (groupsCreated > 0) {
				groupsCreated = 0;
	
				nodes.clear();
				nodes.addAll(sn.getBaseChildren());
				
				
				// Compare every node with every other node
				
				for (int i = 0; i < nodes.size(); i++) {
					BaseNode pivot = nodes.get(i);
					if (!pivot.isVisible()) continue;
					BaseSummaryNode group = null;
					
					if (canceled) throw new RuntimeException(new JobCanceledException());
					
					for (int j = i + 1; j < nodes.size(); j++) {
						BaseNode n = nodes.get(j);
						if (n.getParent() != sn) continue;
						if (!n.isVisible()) continue;
						
						if (checker.canGroup(pivot, n)) {
							if (group == null) {
								groupsCreated++;
								group = graph.newSummaryNode(sn);
								group.moveBaseNodeFromParent(pivot);
							}
							group.moveBaseNodeFromParent(n);
						}
					}
					
					
					// Label
					
					if (group != null && checker instanceof BaseSummaryLabeler) {
						((BaseSummaryLabeler) checker).label(group);
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
