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

package edu.harvard.pass.algorithm;

import java.util.Collection;

import edu.harvard.pass.*;
import edu.harvard.util.job.JobObserver;


/**
 * The ProvRank algorithm
 * 
 * @author Peter Macko
 */
public class ProvRank {

	private PGraph graph;
	private int iterations;
	
	
	/**
	 * Create an instance of ProvRank for a specific provenance graph
	 * 
	 * @param graph the provenance graph
	 */
	public ProvRank(PGraph graph) {
		this.graph = graph;
		this.iterations = 200;
	}
	
	
	/**
	 * Compute the rank
	 * 
	 * Note: This method uses the AUX field in PNode
	 * 
	 * @param observer the job observer (can be null)
	 */
	public void run(JobObserver observer) {
		
		// Initialize
		
		Collection<PNode> nodes = graph.getNodes();
		int N = nodes.size();
		if (N <= 0) return;
		
		if (observer != null) observer.setRange(0, iterations);
		
		
		// Set the initial values
		
		double initial = 1.0 / (double) N;
		
		for (PNode n : nodes) {
			n.setProvRank(initial);
			n.setAux(0);
		}
		
		
		// Approximate the rank using the iterative algorithm
		
		for (int iteration = 0; iteration < iterations; iteration++) {

			if (observer != null) observer.setProgress(iteration);

			
			// Compute AUX

			double X = 0;

			for (PNode n : nodes) {
				
				Collection<PEdge> edges = n.getOutgoingEdges();
				int size = edges.size();

				if (size == 0) {
					X += n.getProvRank() / N;
				}
				else {
					double x = n.getProvRank();
					for (PEdge e : edges) {
						e.getTo().addAux(x);
					}
				}
			}


			// Update ProvRank

			double sum = 0;
			for (PNode n : nodes) {
				
				double r = X + n.getAux();
				sum += r;
				
				n.setProvRank(r);
				n.setAux(0);
			}


			// Normalize

			for (PNode n : nodes) {
				n.setProvRank(n.getProvRank() / sum);
			}
		}
		
		if (observer != null) observer.setProgress(iterations);
	
		
		// Finalize
		
		if (observer != null) observer.makeIndeterminate();
		
		graph.setWasProvRankComputed(true);
		graph.updateProvRankStatistics();
	}
}
