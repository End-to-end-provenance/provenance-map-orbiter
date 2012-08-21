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

package edu.harvard.pass;

import edu.harvard.util.graph.BaseNode;
import edu.harvard.util.graph.SummaryNode;
import edu.harvard.util.graph.WithTimeInterval;


/**
 * A summary node in a provenance graph
 * 
 * @author Peter Macko
 */
public class PSummaryNode extends SummaryNode<PNode, PEdge, PSummaryNode, PGraph> implements WithTimeInterval {

	private static final long serialVersionUID = -8291730443135674627L;

	private double startTime = Double.MAX_VALUE;
	private double endTime = Double.MIN_VALUE;
	private BaseNode startTimeNode = null;
	private BaseNode endTimeNode = null;


	/**
	 * Create an instance of class PSummaryNode
	 * 
	 * @param parent the parent node group (this adds this object to this parent)
	 */
	public PSummaryNode(PSummaryNode parent) {
		super(parent);
	}
	
	
	/**
	 * Create an instance of class PSummaryNode
	 * 
	 * @param graph the parent graph
	 */
	public PSummaryNode(PGraph graph) {
		super(graph);
	}

	
	/**
	 * Get the start time
	 * 
	 * @return the start time
	 */
	public double getStartTime() {
		return startTime;
	}
	
	
	/**
	 * Get the end time
	 * 
	 * @return the end time
	 */
	public double getEndTime() {
		return endTime;
	}
	
	
	/**
	 * Add a child
	 * 
	 * @param child the children node
	 */
	@Override
	protected void addChild(BaseNode child) {
		
		super.addChild(child);
		
		if (child instanceof WithTimeInterval) {
			WithTimeInterval c = (WithTimeInterval) child;
			
			double t = c.getStartTime();
			if (t < startTime) {
				startTime = t;
				startTimeNode = child;
			}
			
			t = c.getEndTime();
			if (t > endTime) {
				endTime = t;
				endTimeNode = child;
			}
		}
	}
	
	
	/**
	 * Remove a child from the collection of children, but do not update the edge cache
	 * 
	 * @param child the children node
	 * @return true if the child was removed, or false if it was not there to begin with
	 */
	@Override
	protected boolean removeChild(BaseNode child) {
		
		if (!super.removeChild(child)) return false;
		
		if (child instanceof WithTimeInterval) {
			if (child == startTimeNode || child == endTimeNode) {
				
				// TODO Make this more efficient!
				
				startTime = Double.MAX_VALUE;
				endTime = Double.MIN_VALUE;
				
				for (BaseNode node : getBaseChildren()) {
					if (node instanceof WithTimeInterval) {
						WithTimeInterval n = (WithTimeInterval) node;
						
						double t = n.getStartTime();
						if (t < startTime) startTime = t;
						
						t = n.getEndTime();
						if (t > endTime) endTime = t;
					}
				}
			}
		}
		
		return true;
	}
}
