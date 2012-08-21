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

package edu.harvard.pass.algorithm;

import java.io.File;
import java.util.*;

import edu.harvard.pass.*;
import edu.harvard.util.Cancelable;
import edu.harvard.util.Utils;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.summarizer.*;
import edu.harvard.util.job.JobCanceledException;
import edu.harvard.util.job.JobObserver;


/**
 * Summarize by file extensions
 * 
 * @author Peter Macko
 */
public class FileExtSummarizer implements ObservableGraphSummarizer, Cancelable {

	private boolean canceled = false;
	
	private int threshold;
	private boolean mustHaveSameInputsOutputs;
	
	
	/**
	 * Create an instance of class FileExtSummarizer
	 */
	public FileExtSummarizer() {
		this.threshold = 4;
		this.mustHaveSameInputsOutputs = true;
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

		if (!(graph instanceof PGraph)) {
			throw new IllegalArgumentException("The graph needs to be an instance of PGraph");
		}
		
		PGraph g = Utils.<PGraph>cast(graph);
	
		canceled = false;
		g.summarizationBegin();
		
		
		// For every summary node that currently exists...
		
		PSummaryNode root = g.getRootSummaryNode();
		Collection<PSummaryNode> summaryNodes = new HashSet<PSummaryNode>();
		root.collectSummaryNodes(summaryNodes);
		
		if (observer != null) {
			if (summaryNodes.size() > 10) {
				observer.setRange(0, summaryNodes.size());
			}
			else {
				observer.makeIndeterminate();
			}
		}
		
		int count = 0;
		
		for (PSummaryNode sn : summaryNodes) {
			
			if (observer != null) {
				if (summaryNodes.size() > 10) {
					observer.setProgress(count);
				}
			}
			
			count++;
			
			if (canceled) throw new RuntimeException(new JobCanceledException());
			
			
			// Group by the file extension
			
			HashMap<String, Vector<PNode>> groups = new HashMap<String, Vector<PNode>>();
			
			for (BaseNode n : sn.getBaseChildren()) {
				
				if (n.getParent() != sn) continue;
				if (!n.isVisible()) continue;
				if (!(n instanceof PNode)) continue;
				PNode node = (PNode) n;
				
				if (node.getObject().getType() != PObject.Type.ARTIFACT) continue;
				if (node.getObject().getName() == null) continue;
				File f = new File(node.getObject().getName());
				
				String ext = null;
				if (ext == null) if (f.getName().indexOf(".so.") > 0) ext = "so";
				if (ext == null) ext = Utils.getExtension(f);
				if (ext == null) continue;
				
				Vector<PNode> v = groups.get(ext);
				if (v == null) {
					v = new Vector<PNode>();
					groups.put(ext, v);
				}
				
				v.add(node);
			}
			
			
			// Process each group
			
			for (String ext : groups.keySet()) {
				Vector<PNode> group = groups.get(ext);
				
				
				// Break up each extension group by the inputs/outputs (if applicable)
				
				Vector<Vector<PNode>> subgroups = new Vector<Vector<PNode>>();
				
				if (mustHaveSameInputsOutputs) {
					for (int i = 0; i < group.size(); i++) {
						PNode n = group.get(i);
						if (n == null) continue;
						
						Vector<PNode> v = new Vector<PNode>();
						v.add(n);
						
						for (int j = i + 1; j < group.size(); j++) {
							PNode m = group.get(j);
							if (m == null) continue;
							
							if (!Utils.areCollectionsEqual(n.getIncomingNodes(), m.getIncomingNodes())) continue;
							if (!Utils.areCollectionsEqual(n.getOutgoingNodes(), m.getOutgoingNodes())) continue;
							
							v.add(m);
							group.set(j, null);
						}
						
						subgroups.add(v);
					}
				}
				else {
					subgroups.add(group);
				}
				
				
				// Make the summary nodes
				
				for (Vector<PNode> v : subgroups) {
					if (v.size() < threshold) continue;
					
					PSummaryNode s = new PSummaryNode(sn);
					for (PNode n : v) s.moveNodeFromAncestor(n);
					
					s.setLabel("*." + ext);
				}
			}
		}
		
		
		// Finish
		
		g.summarizationEnd();
	}

	
	/**
	 * Cancel all computations
	 */
	@Override
	public void cancel() {
		canceled = true;
	}
}
