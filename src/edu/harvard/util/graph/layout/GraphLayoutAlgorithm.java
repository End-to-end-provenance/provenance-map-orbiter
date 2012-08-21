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

package edu.harvard.util.graph.layout;

import edu.harvard.util.graph.*;
import edu.harvard.util.job.JobObserver;


/**
 * A graph layout algorithm
 * 
 * @author Peter Macko
 */
public interface GraphLayoutAlgorithm {

	/**
	 * Initialize the graph layout for the given graph
	 * 
	 * @param graph the input graph
	 * @param levels the number of levels in the hierarchy of summary nodes to precompute 
	 * @param observer the job observer
	 * @return the graph layout
	 */
	public GraphLayout initializeLayout(BaseGraph graph, int levels, JobObserver observer);
	
	/**
	 * Update an existing layout by incrementally expanding the given summary node
	 * 
	 * @param layout the graph to update
	 * @param node the summary node to expand
	 * @param observer the job observer
	 */
	public void updateLayout(GraphLayout layout, BaseSummaryNode node, JobObserver observer);

	/**
	 * Compute the layout for the entire graph
	 * 
	 * @param graph the input graph
	 * @param observer the job observer
	 * @return the graph layout
	 */
	public GraphLayout computeLayout(BaseGraph graph, JobObserver observer);
	
	/**
	 * Determine whether the algorithm produces zoom-based (or zoom-optimized) layouts
	 * 
	 * @return true if it produces zoom-optimized layouts
	 */
	public boolean isZoomOptimized();
}
