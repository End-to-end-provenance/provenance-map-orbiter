/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2010
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

package edu.harvard.util.job;

import edu.harvard.util.Cancelable;
import edu.harvard.util.Pointer;
import edu.harvard.util.graph.layout.*;
import edu.harvard.util.graph.*;


/**
 * The job to compute a graph layout
 * 
 * @author Peter Macko
 */
public class GraphLayoutAlgorithmJob extends AbstractJob {

	private Pointer<BaseGraph> input;
	private GraphLayout result;
	private GraphLayoutAlgorithm algorithm;
	private int levels;
	
	private boolean canceled;


	/**
	 * Constructor of class GraphLayoutAlgorithmJob
	 *
	 * @param algorithm the graph layout algorithm
	 * @param input the pointer to the input
	 * @param levels the number of levels in the hierarchy of summary nodes to precompute (-1 to compute the entire layout) 
	 */
	public GraphLayoutAlgorithmJob(GraphLayoutAlgorithm algorithm, Pointer<BaseGraph> input, int levels) {
		super(levels >= 0 ? "Initializing graph layout" : "Computing graph layout");
		this.algorithm = algorithm;
		this.input = input;
		this.levels = levels;
		result = null;
		canceled = false;
	}


	/**
	 * Constructor of class GraphLayoutAlgorithmJob
	 *
	 * @param algorithm the graph layout algorithm
	 * @param input the pointer to the input
	 */
	public GraphLayoutAlgorithmJob(GraphLayoutAlgorithm algorithm, Pointer<BaseGraph> input) {
		this(algorithm, input, -1);
	}


	/**
	 * Return the result of the job
	 *
	 * @return the result
	 */
	public GraphLayout getResult() {
		return result;
	}
	
	
	/**
	 * Run the job
	 * 
	 * @throws JobException if the job failed
	 * @throws JobCanceledException if the job was canceled
	 */
	public void run() throws JobException {
		
		result = null;
		canceled = false;
		

		// Get the graph
		
		BaseGraph g = input.get();
		if (g == null) throw new JobException("No graph");


		// Compute the graph layout
		
		try {
			if (levels < 0) {
				result = algorithm.computeLayout(g, observer);
			}
			else {
				result = algorithm.initializeLayout(g, levels, observer);
			}
			g.addLayout(result);
		}
		catch (Throwable t) {
			if (canceled) throw new JobCanceledException();
			throw new JobException(t);
		}
	}
	
	
	/**
	 * Determine whether the job can be canceled
	 * 
	 * @return true if the job can be canceled
	 */
	public boolean isCancelable() {
		return algorithm instanceof Cancelable;
	}
	

	/**
	 * Cancel the job (if possible)
	 */
	public void cancel() {
		try {
			if (algorithm != null) {
				canceled = true;
				((Cancelable) algorithm).cancel();
			}
		}
		catch (Exception e) {
			// Silent failover
		}
	}
}
