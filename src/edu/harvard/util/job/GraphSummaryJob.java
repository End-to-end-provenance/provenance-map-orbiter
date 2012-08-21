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

import edu.harvard.util.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.summarizer.*;


/**
 * The job to compute a graph summary
 * 
 * @author Peter Macko
 */
public class GraphSummaryJob extends AbstractJob {

	private Pointer<BaseGraph> input;
	private GraphSummarizer summarizer;
	private boolean canceled;


	/**
	 * Constructor of class GraphSummaryJob
	 *
	 * @param summarizer the graph summarizer
	 * @param input the pointer to the input
	 * @param title the job title
	 */
	public GraphSummaryJob(GraphSummarizer summarizer, Pointer<BaseGraph> input, String title) {
		super(title);
		this.summarizer = summarizer;
		this.input = input;
		this.canceled = false;
	}


	/**
	 * Constructor of class GraphSummaryJob
	 *
	 * @param summarizer the graph summarizer
	 * @param input the pointer to the input
	 */
	public GraphSummaryJob(GraphSummarizer summarizer, Pointer<BaseGraph> input) {
		this(summarizer, input, "Computing graph summary");
	}
	
	
	/**
	 * Run the job
	 * 
	 * @throws JobException if the job failed
	 * @throws JobCanceledException if the job was canceled
	 */
	public void run() throws JobException {

		// Get the graph
		
		BaseGraph g = input.get();
		if (g == null) throw new JobException("No graph");


		// Compute the graph layout
		
		canceled = false;
		
		try {
			if (summarizer instanceof ObservableGraphSummarizer) {
				((ObservableGraphSummarizer) summarizer).summarize(g, observer);
			}
			else {
				summarizer.summarize(g);
			}
			g.getRootBaseSummaryNode().checkConsistency();
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
		return summarizer instanceof Cancelable;
	}
	

	/**
	 * Cancel the job (if possible)
	 */
	public void cancel() {
		try {
			canceled = true;
			if (summarizer != null) Utils.<Cancelable>cast(summarizer).cancel();
		}
		catch (Exception e) {
			// Silent failover
		}
	}
}
