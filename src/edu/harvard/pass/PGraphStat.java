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

package edu.harvard.pass;


/**
 * Provenance graph statistics
 *
 * @author Peter Macko
 */
public class PGraphStat implements java.io.Serializable {
	
	private static final long serialVersionUID = -3920549049463272306L;

	public double timeUnadjustedMin;
	public double timeUnadjustedMax;
	public int fdMin;
	public int fdMax;
	
	public int indegreeMin;
	public int indegreeMax;
	public int outdegreeMin;
	public int outdegreeMax;
	public int degreeMin;
	public int degreeMax;
	
	public int depthMin;
	public int depthMax;
	
	public double subRankMin;
	public double subRankMax;
	public double subRankJumpMin;
	public double subRankJumpMax;
	public double subRankLogJumpMin;
	public double subRankLogJumpMax;
	public double subRankMeanLogJumpMin;
	public double subRankMeanLogJumpMax;
	
	public double provRankMin;
	public double provRankMax;
	public double provRankJumpMin;
	public double provRankJumpMax;
	public double provRankLogJumpMin;
	public double provRankLogJumpMax;
	public double provRankMeanLogJumpMin;
	public double provRankMeanLogJumpMax;


	/**
	 * Constructor for objects of type PGraphStat
	 */
	public PGraphStat() {
		clear();
	}
	
	
	/**
	 * Clear the statistics
	 */
	public void clear() {
		
		timeUnadjustedMin = Double.MAX_VALUE;
		timeUnadjustedMax = Double.MIN_VALUE;
		fdMin = Integer.MAX_VALUE;
		fdMax = Integer.MIN_VALUE;
		
		indegreeMin = Integer.MAX_VALUE;
		indegreeMax = Integer.MIN_VALUE;
		outdegreeMin = Integer.MAX_VALUE;
		outdegreeMax = Integer.MIN_VALUE;
		degreeMin = Integer.MAX_VALUE;
		degreeMax = Integer.MIN_VALUE;
		
		depthMin = Integer.MAX_VALUE;
		depthMax = Integer.MIN_VALUE;
		
		subRankMin = Double.MAX_VALUE;
		subRankMax = Double.MIN_VALUE;
		subRankJumpMin = Double.MAX_VALUE;
		subRankJumpMax = Double.MIN_VALUE;
		subRankLogJumpMin = Double.MAX_VALUE;
		subRankLogJumpMax = Double.MIN_VALUE;
		subRankMeanLogJumpMin = Double.MAX_VALUE;
		subRankMeanLogJumpMax = Double.MIN_VALUE;
		
		provRankMin = Double.MAX_VALUE;
		provRankMax = Double.MIN_VALUE;
		provRankJumpMin = Double.MAX_VALUE;
		provRankJumpMax = Double.MIN_VALUE;
		provRankLogJumpMin = Double.MAX_VALUE;
		provRankLogJumpMax = Double.MIN_VALUE;
		provRankMeanLogJumpMin = Double.MAX_VALUE;
		provRankMeanLogJumpMax = Double.MIN_VALUE;
	}


	/**
	 * Return the smallest timestamp, unadjusted relative to the time base in PGraph
	 * 
	 * @return the smallest timestamp
	 */
	public double getTimeUnadjustedMin() {
		return timeUnadjustedMin > timeUnadjustedMax ? 0 : timeUnadjustedMin;
	}


	/**
	 * Return the largest timestamp, unadjusted relative to the time base in PGraph
	 * 
	 * @return the largest timestamp
	 */
	public double getTimeUnadjustedMax() {
		return timeUnadjustedMin > timeUnadjustedMax ? 0 : timeUnadjustedMax;
	}
}
