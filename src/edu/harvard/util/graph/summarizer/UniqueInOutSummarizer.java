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
 * Summarize using unique input/output relationships
 * 
 * @author Peter Macko
 */
public class UniqueInOutSummarizer implements ObservableGraphSummarizer, Cancelable {

	private boolean canceled = false;
	
	private int threshold;
	
	
	/**
	 * Create an instance of class UniqueInOutSummarizer
	 */
	public UniqueInOutSummarizer() {
		this.threshold = 4;
	}
	
	
	/**
	 * Get the name of the summarization algorithm
	 * 
	 * @return the name of the summarization algorithm
	 */
	public String getName() {
		return "Unique In/Out Relationships";
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
			
			if (canceled) throw new RuntimeException(new JobCanceledException());
			
			
			// Find suitable candidates for summarization
			
			HashMap<BaseNode, Vector<BaseNode>> candidates = new HashMap<BaseNode, Vector<BaseNode>>();
			
			for (BaseNode n : sn.getBaseChildren()) {
				
				if (n.getParent() != sn) continue;
				if (!n.isVisible()) continue;
				
				Collection<BaseNode> incoming = n.getVisibleIncomingBaseNodes();
				Collection<BaseNode> outgoing = n.getVisibleOutgoingBaseNodes();
				
				if (incoming.size() + outgoing.size() < threshold) continue;
				
				Vector<BaseNode> v = new Vector<BaseNode>();
				v.add(n);
				
				for (BaseNode x : incoming) {
					if (x.getParent() != sn) continue;
					if (x.getVisibleOutgoingBaseNodes().size() != 1) continue;
					if (x.getVisibleIncomingBaseNodes().size() != 0) continue;
					v.add(x);
				}
				for (BaseNode x : outgoing) {
					if (x.getParent() != sn) continue;
					if (x.getVisibleIncomingBaseNodes().size() != 1) continue;
					if (x.getVisibleOutgoingBaseNodes().size() != 0) continue;
					v.add(x);
				}
				
				if (v.size() >= threshold + 1 && v.size() != sn.getBaseChildren().size()) {
					candidates.put(n, v);
				}
			}
			
			
			while (true) {
				
				// Find the best candidate
				
				BaseNode bestCandidate = null;
				int bestCanditateScore = 0;
			
				for (Map.Entry<BaseNode, Vector<BaseNode>> x : candidates.entrySet()) {
					if (x.getValue().size() > bestCanditateScore) {
						bestCanditateScore = x.getValue().size();
						bestCandidate = x.getKey();
					}					
				}
				
				if (bestCandidate == null) break;
				
				
				// Summarize the best candidate
				
				BaseSummaryNode s = graph.newSummaryNode(sn);
				for (BaseNode n : candidates.remove(bestCandidate)) {
					candidates.remove(n);
					s.moveBaseNodeFromAncestor(n);
				}
				
				s.setLabel(bestCandidate.getLabel());
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
