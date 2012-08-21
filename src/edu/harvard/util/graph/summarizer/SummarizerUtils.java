/*
 * Provenance Aware Storage System - Java Utilities
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

import java.util.Collection;
import java.util.HashSet;

import edu.harvard.util.Utils;
import edu.harvard.util.graph.*;


/**
 * Miscellaneous utility methods useful for graph summarizations
 * 
 * @author Peter Macko
 */
public class SummarizerUtils {

	
	/**
	 * Given a mapping of some graph nodes to summary nodes, assign all other
	 * nodes to the appropriate summary nodes
	 * 
	 * @param graph the graph
	 */
	public static void assignUnassigned(BaseGraph graph) {
		
		BaseSummaryNode root = graph.getRootBaseSummaryNode();
		
		
		// Add nodes to the most common ancestor of their relatives
		
		boolean done = false;
		while (!done) {
			done = true;
				
			for (BaseNode n : graph.getBaseNodes()) {
				
				if (n.getParent() != root) continue;
				if (n.getOutgoingBaseEdges().isEmpty() && n.getIncomingBaseEdges().isEmpty()) continue;
				
				
				// Determine the common ancestor of all the process nodes linked with n
				
				BaseNode c = null;
				for (BaseEdge e : n.getIncomingBaseEdges()) {
					BaseNode x = e.getBaseFrom();
					if (x.getParent() == root) continue;
					
					if (c == null) {
						c = x;
						continue;
					}
					
					c = x.getCommonAncestor(c);
				}
				for (BaseEdge e : n.getOutgoingBaseEdges()) {
					BaseNode x = e.getBaseTo();
					if (x.getParent() == root) continue;
					
					if (c == null) {
						c = x;
						continue;
					}
					
					c = x.getCommonAncestor(c);
				}
				
				if (c == null) continue;
				if (!(c instanceof BaseSummaryNode)) c = c.getParent();
				if (c == null) continue;
				if (c == root) continue;
				
				BaseSummaryNode s = Utils.cast(c);
				
				
				// Move the node to the common ancestor
				
				s.moveBaseNodeFromAncestor(n);
				done = false;
			}
		}
	}
	
	
	/**
	 * Remove summary nodes with a small number of visible nodes (or no nodes at all)
	 * 
	 * @param graph the graph
	 * @param threshold the largest summary group to remove
	 */
	public static void removeSmallSummaries(BaseGraph graph, int threshold) {
		
		boolean done = false;
		while (!done) {
			done = true;
			
			
			// Collect all summary nodes
			
			BaseSummaryNode root = graph.getRootBaseSummaryNode();
			Collection<BaseSummaryNode> summaryNodes = new HashSet<BaseSummaryNode>();
			root.collectBaseSummaryNodes(summaryNodes);
			
			
			// Check each summary node
			
			for (BaseSummaryNode s : summaryNodes) {
				
				if (s.getBaseChildren().size() > threshold) {
					int count = 0; 
					for (BaseNode n : s.getBaseChildren()) {
						if (n.isVisible()) {
							count++;
							if (count > threshold) break;
						}
					}
					if (count > threshold) continue;
				}
				
				
				// Root node is special - move from its summary nodes

				if (s == root) {
					
					HashSet<BaseNode> children = new HashSet<BaseNode>();
					children.addAll(s.getBaseChildren());
					
					for (BaseNode n : children) {
						if (n instanceof BaseSummaryNode) {
							
							HashSet<BaseNode> children2 = new HashSet<BaseNode>();
							children2.addAll(((BaseSummaryNode) n).getBaseChildren());
							
							for (BaseNode x : children2) {
								s.moveBaseNodeFromChild(x);
								done = false;
							}
						}
					}
					
					continue;
				}

				
				// Move the child nodes
				
				if (!s.getBaseChildren().isEmpty()) {
					HashSet<BaseNode> children = new HashSet<BaseNode>();
					children.addAll(s.getBaseChildren());
					
					for (BaseNode n : children) {
						s.getParent().moveBaseNodeFromChild(n);
					}
				}
				
				
				// Unlink the summary node
				
				s.unlinkEmpty();
				done = false;
			}
		}
	}
	
	
	/**
	 * Remove summary nodes with only one visible node inside them, or with nodes at all
	 * 
	 * @param graph the graph
	 */
	public static void removeSingletonSummaries(BaseGraph graph) {
		removeSmallSummaries(graph, 1);
	}
}
