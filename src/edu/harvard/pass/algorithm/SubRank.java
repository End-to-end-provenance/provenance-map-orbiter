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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.harvard.pass.*;
import edu.harvard.util.job.JobObserver;


/**
 * The SubRank algorithm
 * 
 * @author Peter Macko
 */
public class SubRank {

	private PGraph graph;
	
	
	/**
	 * Create an instance of SubRank for a specific provenance graph
	 * 
	 * @param graph the provenance graph
	 */
	public SubRank(PGraph graph) {
		this.graph = graph;
	}
	
	
	/**
	 * Compute the rank
	 * 
	 * Note: This method uses the AUX field in PNode
	 * 
	 * @param observer the job observer (can be null)
	 */
	public void run(JobObserver observer) {
		
		// Initialize the job
		
		Collection<PNode> nodes = graph.getNodes();
		
		
		/*
		 * Fast SubRank Algorithm
		 * 
		 * Analysis:
		 * 
		 *   O(n)    runtime
		 *   O(n^2)  memory in the worst case
		 * 
		 *   
		 * Algorithm:
		 * 
		 *   Active_nodes := nodes with no incoming edges
		 *   for each n in Active_nodes
		 *     Unactivated_incoming_edge_count(n) := Indegree(n)
		 *     Subgraph_set(n) := empty set
		 *   end
		 *   
		 *   Processed_nodes_count := 0
		 *   
		 *   while Active_nodes is not empty
		 *     New_active_nodes := empty set
		 *     
		 *     for each n in Active_nodes
		 *       Subgraph_count(n) := 1 + |Subgraph_set(n)|
		 *       Processed_nodes_count += 1
		 *       
		 *       for each outgoing edge e of n
		 *         m := the other end of e
		 *         Unactivated_incoming_edge_count(m) -= 1
		 *         Add n to Subgraph_set(m)
		 *         Add Subgraph_set(n) to Subgraph_set(m)
		 *         if Unactivated_incoming_edge_count(m) == 0
		 *           Add m to New_active_nodes
		 *         end
		 *       end
		 *     end
		 *     
		 *     Active_nodes := New_active_nodes
		 *   end
		 *   
		 *   if Processed_nodes_count != number of nodes
		 *     fail because a cycle has been detected
		 *   end
		 *   
		 *   for each node n:
		 *     SubRank(n) := Subgraph_count(n) / number of nodes
		 *   end
		 */
		
		
		// Initialize the computation (AUX = count of unactivated incoming edges)
		
		HashMap<PNode, Set<PNode>> subgraphSets = new HashMap<PNode, Set<PNode>>();
		Queue<PNode> active = new LinkedList<PNode>();
		
		int numNodes = 0;
		
		for (PNode n : nodes) {
			if (!n.isVisible()) {
				n.setSubRank(1 / (double) nodes.size());
			}
			
			numNodes++;
			n.setAux(n.getIncomingEdges().size());
			
			if (n.getIncomingEdges().isEmpty()) {
				active.add(n);
				subgraphSets.put(n, new HashSet<PNode>());
			}
		}
		
		if (numNodes <= 0) return;
		if (observer != null) observer.setRange(0, numNodes);
		
		
		// Compute
		
		int processedNodes = 0;
		
		while (!active.isEmpty()) {
			PNode n = active.poll();
			processedNodes++;
			
			Set<PNode> nSubgraphSet = subgraphSets.remove(n);
			nSubgraphSet.add(n);
			n.setSubRank(nSubgraphSet.size() / (double) numNodes);
			boolean passedDown = false;
			
			for (PEdge e : n.getOutgoingEdges()) {
				PNode m = e.getTo();
				m.addAux(-1);
				if (n == m) {
					throw new IllegalStateException("The graph contains a self-loop");
				}
				
				Set<PNode> mSubgraphSet = subgraphSets.get(m);
				if (mSubgraphSet == null) {
					if (passedDown) {
						subgraphSets.put(m, new HashSet<PNode>(nSubgraphSet));
					}
					else {
						subgraphSets.put(m, nSubgraphSet);
						passedDown = true;
					}
				}
				else {
					mSubgraphSet.addAll(nSubgraphSet);
				}
				
				if (m.getAux() < 0.0001) {
					active.add(m);
				}
			}
			
			if (observer != null) observer.setProgress(processedNodes);
		}

		if (processedNodes != numNodes) {
			throw new IllegalStateException("The graph contains a cycle");
		}
		
		
		// Finalize
		
		if (observer != null) observer.makeIndeterminate();
		
		graph.setWasSubRankComputed(true);
		graph.updateSubRankStatistics();
	}
}
