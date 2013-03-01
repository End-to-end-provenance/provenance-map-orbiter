/*
 * Provenance Aware Storage System - Java Utilities
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

package edu.harvard.pass.job;

import edu.harvard.pass.algorithm.SubRank;
import edu.harvard.pass.*;
import edu.harvard.util.job.*;
import edu.harvard.util.*;


/**
 * The job to compute SubRank
 * 
 * @author Peter Macko
 */
public class SubRankJob extends AbstractJob {

	private Pointer<PGraph> input;


	/**
	 * Constructor of class SubRankJob
	 *
	 * @param input the input pointer
	 */
	public SubRankJob(Pointer<PGraph> input) {
		super("Computing SubRank");
		this.input = input;
	}
	
	
	/**
	 * Run the job
	 * 
	 * @throws JobException if the job failed
	 * @throws JobCanceledException if the job was canceled
	 */
	public void run() throws JobException {
		

		// Get the graph
		
		PGraph g = input.get();
		if (g == null) throw new JobException("No graph");


		// Compute SubRank
		
		try {
			SubRank A = new SubRank(g);
			A.run(observer);
		}
		catch (Throwable t) {
			throw new JobException(t);
		}
	}
}
